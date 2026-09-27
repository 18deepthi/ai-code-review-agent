$ErrorActionPreference = "Stop"

$code = "package com.example.service; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.stereotype.Service; @Service public class OrderProcessorService { @Autowired private OrderRepository orderRepository; }"

# Pristine bank setup
Invoke-RestMethod -Uri "http://localhost:8080/api/memories/team-alpha" -Method Delete | Out-Null
Invoke-RestMethod -Uri "http://localhost:8080/api/memories/team-backend-core" -Method Delete | Out-Null

# Step 1: Baseline Review under team-alpha (Pristine, 0 memories)
$body1 = @{ teamId = 'team-alpha'; codeSnippet = $code; language = 'java' } | ConvertTo-Json
$alpha1 = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -Body $body1 -ContentType "application/json"
Write-Host "team-alpha initial review:"
Write-Host "  Total Issues : $($alpha1.totalIssuesCount)"
Write-Host "  Skipped      : $($alpha1.skippedCount)"
Write-Host "  Actionable   : $($alpha1.activeIssuesCount)"

# Step 2: Reject suggestion under team-alpha
$fbBody = @{
  teamId = 'team-alpha'
  reviewId = $alpha1.reviewId
  commentId = $alpha1.comments[0].id
  decision = 'rejected'
  rule = 'Field injection is accepted for legacy order processor services'
  note = 'Team-alpha architectural exception'
  sourceSnippetExcerpt = $code
} | ConvertTo-Json
$fb = Invoke-RestMethod -Uri "http://localhost:8080/api/feedback" -Method Post -Body $fbBody -ContentType "application/json"
Write-Host "Feedback saved: $($fb.message)"

# Step 3: Re-review under team-alpha
$alpha2 = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -Body $body1 -ContentType "application/json"
Write-Host "`nteam-alpha re-review (Memory Active):"
Write-Host "  Total Issues : $($alpha2.totalIssuesCount)"
Write-Host "  Skipped      : $($alpha2.skippedCount) (EXPECTED: 1)"
Write-Host "  Actionable   : $($alpha2.activeIssuesCount) (EXPECTED: 0)"
Write-Host "  Reason       : $($alpha2.comments[0].memoryReason)"

# Step 4: Switch to team-backend-core and run EXACT same scenario
$body2 = @{ teamId = 'team-backend-core'; codeSnippet = $code; language = 'java' } | ConvertTo-Json
$core = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -Body $body2 -ContentType "application/json"
Write-Host "`nteam-backend-core review (FRESH - Zero Feedback):"
Write-Host "  Total Issues : $($core.totalIssuesCount) (EXPECTED: 1)"
Write-Host "  Skipped      : $($core.skippedCount) (EXPECTED: 0)"
Write-Host "  Actionable   : $($core.activeIssuesCount) (EXPECTED: 1)"
Write-Host "  Recalled Mem : $($core.recalledMemories.Count) (EXPECTED: 0)"
Write-Host "  Issue Flagged: $($core.comments[0].issue)"
Write-Host "  Skipped Due to Memory: $($core.comments[0].skippedDueToMemory) (EXPECTED: False)"
