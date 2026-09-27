# Hindsight ReviewerAI

An AI-powered Java code review agent with persistent, team-scoped memory. 
Instead of repeating the same feedback every time, this agent remembers 
what your team has already decided — accepted conventions and overridden 
suggestions — using [Hindsight](https://hindsight.vectorize.io/) as its 
long-term memory layer.

## The Problem
Code review is repetitive. A reviewer (human or AI) flags the same issue 
over and over, even after a team has explicitly decided it's not a 
problem for them. This wastes time re-explaining team conventions to 
every new reviewer or new hire.

## The Solution
This agent reviews Java code, and every time a suggestion is rejected 
("Reject & Remember"), it retains that decision as a team-scoped memory 
in Hindsight. On future reviews, it recalls relevant memories before 
generating feedback, and silently skips issues the team has already 
overridden — while still flagging genuinely new issues.

## How Hindsight Is Used
- **Recall**: Before generating a review, the backend queries Hindsight 
  for memories scoped to the active team, based on the code's detected 
  issue patterns.
- **Retain**: When a user clicks "Reject & Remember," the rejected 
  suggestion is written to Hindsight as a team-scoped preference.
- **Isolation**: Memories are scoped per team (via `teamId`), so 
  different teams maintain independent, non-overlapping preferences.
- Over multiple interactions, review quality visibly improves — 
  generic flags become team-aware skips, citing exactly which past 
  decision justified the skip.

## Architecture

```mermaid
graph TD
    User([Developer / Tech Lead]) -->|1. Submit Java Code| Frontend[React + Vite Frontend]
    Frontend -->|2. POST /api/review| Backend[Spring Boot 3.x Backend]
    
    subgraph Spring Boot Backend
        Ctrl[ReviewController]
        ReviewSvc[CodeReviewService]
        FeatureExt[Snippet Characteristics Extractor]
        HindsightCli[HindsightClient Service]
        GroqCli[GroqClient with Retries]
        HistStore[Submission History Store]
    end

    Backend -->|3. Query based on characteristics| HindsightCloud[(Hindsight Cloud API)]
    HindsightCloud -->|4. Recalled Team Preferences| Backend
    Backend -->|5. Prompt: Code + Recalled Memories| GroqLLM[Groq API: openai/gpt-oss-120b]
    GroqLLM -->|6. Structured Review Comments| Backend
    Backend -->|7. Response with Skipped Badges| Frontend
    
    User -->|8. Accept / Reject Comment| Frontend
    Frontend -->|9. POST /api/feedback| Backend
    Backend -->|10. Retain New Memory| HindsightCloud
```

## Tech Stack
- **Backend:** Java 17, Spring Boot 3.3.x, Maven
- **Frontend:** React + Vite
- **Memory:** Hindsight Cloud
- **LLM:** Groq (`openai/gpt-oss-120b`, fallback `qwen/qwen3.8-27b`)

## Setup & Run

### Prerequisites
- Java 17
- Maven
- Node.js + npm
- Hindsight Cloud account + API key
- Groq API key

### Backend
```bash
cd backend
cp .env.example .env
# add your HINDSIGHT_API_KEY, HINDSIGHT_BASE_URL, and GROQ_API_KEY to .env
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

Open the local URL shown by Vite (usually `http://localhost:5173`).

## Demo Flow
1. Select a team and load a synthetic Java scenario
2. Click "Review Code with Memory" — see it flag issues generically
3. Click "Reject & Remember" on a suggestion
4. Load a similar scenario — see it now skip the issue, citing the learned team preference
5. Check the Memory Dashboard and History tabs to see accumulated learning over time

## Team
- Deepthi Nakka
