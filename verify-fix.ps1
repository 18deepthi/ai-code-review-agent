Write-Output "=== STEP 1: CLEAR TEAM-ALPHA MEMORIES ==="
$delResp = Invoke-RestMethod -Uri "http://localhost:8080/api/memories/team-alpha" -Method Delete
Write-Output "Delete response: Success"

Write-Output "`n=== STEP 2: VERIFY ZERO MEMORIES ==="
$memsAfterClear = Invoke-RestMethod -Uri "http://localhost:8080/api/memories/team-alpha" -Method Get
Write-Output "Stored memories count: $($memsAfterClear.Count)"

Write-Output "`n=== STEP 3: RUN INITIAL REVIEW FOR TEAM-ALPHA ==="
$code = @"
@Service
public class OrderProcessorService {
    @Autowired
    private OrderRepository orderRepository;

    public void process(Order order) {
        orderRepository.save(order);
    }
}
"@

$reviewBody = @{
    codeSnippet = $code
    teamId = "team-alpha"
    language = "java"
} | ConvertTo-Json

$initialReview = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -ContentType "application/json" -Body $reviewBody
Write-Output "Initial Review: Total=$($initialReview.totalIssuesCount), Skipped=$($initialReview.skippedCount), Active=$($initialReview.activeIssuesCount)"
$fieldInjComment = $initialReview.comments | Where-Object { $_.ruleKey -eq "field_injection" -or $_.issue -match "Field injection" } | Select-Object -First 1
Write-Output "Field Injection Comment found: $($fieldInjComment.issue) | Skipped: $($fieldInjComment.skippedDueToMemory)"

Write-Output "`n=== STEP 4: REJECT FIELD INJECTION ONCE ==="
$feedbackBody = @{
    teamId = "team-alpha"
    reviewId = $initialReview.reviewId
    commentId = $fieldInjComment.id
    decision = "rejected"
    rule = "Field injection permitted for legacy service classes"
    ruleKey = "field_injection"
    note = "We allow field injection in legacy services"
} | ConvertTo-Json

$feedbackResp = Invoke-RestMethod -Uri "http://localhost:8080/api/feedback" -Method Post -ContentType "application/json" -Body $feedbackBody
Write-Output "Feedback 1 result: $($feedbackResp.message)"

Write-Output "`n=== STEP 5: VERIFY EXACTLY ONE MEMORY STORED ==="
$memsAfterFeedback1 = Invoke-RestMethod -Uri "http://localhost:8080/api/memories/team-alpha" -Method Get
Write-Output "Stored memories count: $($memsAfterFeedback1.Count)"
foreach ($m in $memsAfterFeedback1) {
    Write-Output " - Memory: $($m.rule)"
}

Write-Output "`n=== STEP 6: TEST DEDUPLICATION (REJECT SAME ISSUE AGAIN) ==="
$feedbackResp2 = Invoke-RestMethod -Uri "http://localhost:8080/api/feedback" -Method Post -ContentType "application/json" -Body $feedbackBody
Write-Output "Feedback 2 result: $($feedbackResp2.message)"
$memsAfterFeedback2 = Invoke-RestMethod -Uri "http://localhost:8080/api/memories/team-alpha" -Method Get
Write-Output "Stored memories count after repeat: $($memsAfterFeedback2.Count) (Must still be 1!)"

Write-Output "`n=== STEP 7: RE-RUN SCENARIO FOR TEAM-ALPHA ==="
$secondReview = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -ContentType "application/json" -Body $reviewBody
Write-Output "Second Review Results:"
Write-Output "Total Issues: $($secondReview.totalIssuesCount)"
Write-Output "Skipped Count: $($secondReview.skippedCount)"
Write-Output "Active Issues: $($secondReview.activeIssuesCount)"
Write-Output "Recalled Memories: $($secondReview.recalledMemories.Count)"

Write-Output "`n=== STEP 8: COMMENTS BREAKDOWN ==="
foreach ($c in $secondReview.comments) {
    Write-Output " - [$($c.ruleKey)] $($c.issue)"
    Write-Output "   Skipped: $($c.skippedDueToMemory)"
    if ($c.skippedDueToMemory) {
        Write-Output "   Reason: $($c.memoryReason)"
    }
}
