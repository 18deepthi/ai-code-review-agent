const API_BASE = '/api';

export async function submitCodeReview(teamId, codeSnippet, language = 'java') {
  const response = await fetch(`${API_BASE}/review`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ teamId, codeSnippet, language })
  });
  if (!response.ok) {
    throw new Error(`Review request failed: ${response.status} ${response.statusText}`);
  }
  return response.json();
}

export async function sendFeedback(payload) {
  const response = await fetch(`${API_BASE}/feedback`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error(`Feedback submission failed: ${response.status} ${response.statusText}`);
  }
  return response.json();
}

export async function fetchTeamMemories(teamId) {
  const response = await fetch(`${API_BASE}/memories/${encodeURIComponent(teamId)}`);
  if (!response.ok) {
    throw new Error(`Failed to load memories: ${response.status}`);
  }
  return response.json();
}

export async function fetchTeamSubmissions(teamId) {
  const response = await fetch(`${API_BASE}/submissions/${encodeURIComponent(teamId)}`);
  if (!response.ok) {
    throw new Error(`Failed to load submissions: ${response.status}`);
  }
  return response.json();
}

export async function fetchHealthStatus() {
  try {
    const response = await fetch(`${API_BASE}/diagnostic/health`);
    if (!response.ok) return { status: 'DEGRADED', groqConfigured: false, hindsightConfigured: false };
    return response.json();
  } catch (err) {
    return { status: 'OFFLINE', groqConfigured: false, hindsightConfigured: false };
  }
}
