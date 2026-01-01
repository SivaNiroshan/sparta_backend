import React from 'react'
import DashPlayer from './components/DashPlayer'
import './App.css'

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <h1>Sparta Video Streaming</h1>
        <p>DASH Protocol Video Player</p>
      </header>
      <main className="App-main">
        <DashPlayer />
      </main>
    </div>
  )
}

export default App

