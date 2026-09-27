# Verification Script: Memory Isolation Between Teams
$ErrorActionPreference = "Stop"

$code = "package com.example.service; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.stereotype.Service; @Service public class OrderService { @Autowired private OrderRepo repo; }"

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "  Step 1: Baseline Review for team-alpha" -ForegroundColor Cyan
Write-Host "========================================================"
$revA1 = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -Body (@{ teamId = 'team-alpha'; codeSnippet = $code; language = 'java' } | ConvertTo-Json) -ContentType "application/json"
Write-Host "team-alpha: Total Issues = $($revA1.totalIssuesCount), Skipped = $($revA1.skippedCount), Actionable = $($revA1.activeIssuesCount)"
Write-Host "Flagged Issue: $($revA1.comments[0].issue)" -ForegroundColor Yellow

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "  Step 2: Reject Field Injection for team-alpha" -ForegroundColor Cyan
Write-Host "========================================================"
$fb = Invoke-RestMethod -Uri "http://localhost:8080/api/feedback" -Method Post -Body (@{ teamId = 'team-alpha'; reviewId = $revA1.reviewId; commentId = $revA1.comments[0].id; decision = 'rejected'; rule = 'Field injection is accepted across existing order management modules'; note = 'Lead architect exemption for team-alpha' } | ConvertTo-Json) -ContentType "application/json"
Write-Host "Hindsight Feedback Response: $($fb.message)" -ForegroundColor Magenta

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "  Step 3: Re-review for team-alpha (Memory MUST Apply)" -ForegroundColor Cyan
Write-Host "========================================================"
$revA2 = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -Body (@{ teamId = 'team-alpha'; codeSnippet = $code; language = 'java' } | ConvertTo-Json) -ContentType "application/json"
Write-Host "team-alpha Re-review: Total Issues = $($revA2.totalIssuesCount), Skipped = $($revA2.skippedCount), Actionable = $($revA2.activeIssuesCount)" -ForegroundColor Green
Write-Host "team-alpha Skipped Due to Memory: $($revA2.comments[0].skippedDueToMemory)" -ForegroundColor Green
Write-Host "team-alpha Memory Reason: $($revA2.comments[0].memoryReason)" -ForegroundColor Green

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "  Step 4: Review EXACT SAME CODE for team-backend-core" -ForegroundColor Cyan
Write-Host "========================================================"
$revB = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -Body (@{ teamId = 'team-backend-core'; codeSnippet = $code; language = 'java' } | ConvertTo-Json) -ContentType "application/json"
Write-Host "team-backend-core Review:"
Write-Host "  Total Issues Count  : $($revB.totalIssuesCount)"
Write-Host "  Skipped Count       : $($revB.skippedCount)"
Write-Host "  Actionable Issues   : $($revB.activeIssuesCount)"
Write-Host "  Recalled Memories   : $($revB.recalledMemories.Count)"
Write-Host "  Skipped Due to Mem  : $($revB.comments[0].skippedDueToMemory)"
Write-Host "  Fresh Flagged Issue : $($revB.comments[0].issue)" -ForegroundColor Yellow

if ($revB.skippedCount -eq 0 -and $revB.activeIssuesCount -eq 1 -and $revB.comments[0].skippedDueToMemory -eq $false) {
  Write-Host "`n>>> SUCCESS: Memory isolation strictly verified! team-backend-core shows Actionable: 1, Skipped: 0. <<<`n" -ForegroundColor Green
} else {
  Write-Host "`n>>> FAILURE: Cross-team contamination detected! <<<`n" -ForegroundColor Red
}
