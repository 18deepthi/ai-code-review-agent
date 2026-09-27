# Demonstration Runner Script for AI Code Reviewer with Persistent Memory
$ErrorActionPreference = "Stop"

$scenarios = @(
  @{
    teamId = "team-alpha";
    title = "Field Injection Convention";
    code = "package com.example.service; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.stereotype.Service; @Service public class OrderProcessorService { @Autowired private OrderRepository orderRepository; public void process(Long id) { orderRepository.findById(id); } }";
    rule = "Field injection (@Autowired) is accepted for legacy order processor services";
    note = "Approved architectural exception by lead architect"
  },
  @{
    teamId = "team-alpha";
    title = "N+1 Query Throttling";
    code = "package com.example.billing; import java.util.*; public class InvoiceAggregator { public List<String> getInvoices(List<Customer> customers) { List<String> res = new ArrayList<>(); for (Customer c : customers) { res.addAll(repository.findById(c.getId())); } return res; } }";
    rule = "N+1 loop querying is allowed for customer collections capped at <= 5 items";
    note = "Downstream pagination guarantees collection size is <= 5"
  },
  @{
    teamId = "team-alpha";
    title = "Swallowed Exception Strategy";
    code = "package com.example.cache; public class CacheJanitor { public void purge(String prefix) { try { cache.evict(prefix); } catch (Exception e) {} } }";
    rule = "Empty catch blocks permitted in best-effort background cache cleanup";
    note = "Cache cleanup is non-critical and must never disrupt request execution"
  }
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  AI Code Reviewer - Hindsight Learning Demo Progression" -ForegroundColor Cyan
Write-Host "==========================================================`n" -ForegroundColor Cyan

foreach ($s in $scenarios) {
  Write-Host "----------------------------------------------------------" -ForegroundColor Yellow
  Write-Host "SCENARIO: $($s.title)" -ForegroundColor Yellow
  Write-Host "----------------------------------------------------------"

  # Step 1: Baseline Review
  $reviewReq = @{
    teamId = $s.teamId
    codeSnippet = $s.code
    language = "java"
  } | ConvertTo-Json

  $rev1 = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -Body $reviewReq -ContentType "application/json"
  Write-Host "[Run 1 - Baseline] Issues Flagged: $($rev1.totalIssuesCount) | Skipped: $($rev1.skippedCount) (Generic baseline, no memory)"
  $targetComment = $rev1.comments[0]
  Write-Host "  --> Flagged Issue: $($targetComment.issue)" -ForegroundColor Red

  # Step 2: Human Override Feedback
  $feedbackReq = @{
    teamId = $s.teamId
    reviewId = $rev1.reviewId
    commentId = $targetComment.id
    decision = "rejected"
    rule = $s.rule
    note = $s.note
    sourceSnippetExcerpt = $s.code.Substring(0, [Math]::Min(120, $s.code.Length))
  } | ConvertTo-Json

  $fb = Invoke-RestMethod -Uri "http://localhost:8080/api/feedback" -Method Post -Body $feedbackReq -ContentType "application/json"
  Write-Host "[Feedback] Saved to Hindsight: $($fb.message)" -ForegroundColor Magenta

  # Step 3: Second Review (Memory Applied)
  $rev2 = Invoke-RestMethod -Uri "http://localhost:8080/api/review" -Method Post -Body $reviewReq -ContentType "application/json"
  Write-Host "[Run 2 - Memory Active] Issues: $($rev2.totalIssuesCount) | Skipped by Memory: $($rev2.skippedCount)" -ForegroundColor Green
  $skippedComment = $rev2.comments | Where-Object { $_.skippedDueToMemory -eq $true } | Select-Object -First 1
  if ($skippedComment) {
    Write-Host "  --> SKIPPED BADGE: $($skippedComment.memoryReason)" -ForegroundColor Green
  }
  Write-Host ""
}

# Summary Check
$memories = Invoke-RestMethod -Uri "http://localhost:8080/api/memories/team-alpha" -Method Get
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "FINAL MEMORY BANK STATUS (team-alpha): $($memories.Count) Learned Preferences" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
foreach ($m in $memories) {
  Write-Host "- [$($m.decision.ToUpper())] $($m.rule)" -ForegroundColor Gray
}
