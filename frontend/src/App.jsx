import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import SubmitPage from './pages/SubmitPage';
import MemoryDashboardPage from './pages/MemoryDashboardPage';
import HistoryPage from './pages/HistoryPage';
import { fetchHealthStatus } from './services/api';

export default function App() {
  const [activeTab, setActiveTab] = useState('submit');
  const [teamId, setTeamId] = useState('team-alpha');
  const [health, setHealth] = useState(null);

  const checkHealth = async () => {
    const data = await fetchHealthStatus();
    setHealth(data);
  };

  useEffect(() => {
    checkHealth();
    const interval = setInterval(checkHealth, 15000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="app-container">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        teamId={teamId}
        setTeamId={setTeamId}
        health={health}
      />

      <main className="main-content">
        {activeTab === 'submit' && (
          <SubmitPage
            teamId={teamId}
            setTeamId={setTeamId}
            onFeedbackGiven={() => {
              // optional callback
            }}
          />
        )}

        {activeTab === 'memory' && (
          <MemoryDashboardPage teamId={teamId} />
        )}

        {activeTab === 'history' && (
          <HistoryPage teamId={teamId} />
        )}
      </main>
    </div>
  );
}
