<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { connect, disconnect, sendDrawEvent, type DrawEvent } from './services/websocket';
  import Canvas from './components/Canvas.svelte';

  let connectionStatus = 'Disconnected';
  let messages: DrawEvent[] = [];
  let username = '';
  let isUsernameSet = false;
  let canvasComponent: Canvas;

  // Handle received messages
  function handleMessage(message: DrawEvent) {
    messages = [...messages, message];
    console.log('Received message:', message);
  }

  // Handle received history batch
  function handleHistoryBatch(historyEvents: DrawEvent[]) {
    console.log(`Processing history batch with ${historyEvents.length} events`);
    
    // Process the history batch directly in the Canvas component
    if (canvasComponent) {
      canvasComponent.processHistoryBatch(historyEvents);
    } else {
      console.error("Canvas component not available yet");
      // Store the history for later processing
      messages = [...historyEvents];
    }
  }

  // Send a test message
  function sendTestMessage() {
    const testEvent: DrawEvent = {
      username,
      type: 'START',
      x: Math.random() * 100,
      y: Math.random() * 100,
      color: '#000000',
      lineWidth: 2,
      isEraser: false
    };
    
    sendDrawEvent(testEvent);
  }

  async function setUsername() {
    if (username.trim()) {
      isUsernameSet = true;
      try {
        connectionStatus = 'Connecting...';
        await connect(username, handleMessage, handleHistoryBatch);
        connectionStatus = 'Connected';
      } catch (error) {
        connectionStatus = `Connection failed: ${error}`;
        console.error('WebSocket connection error:', error);
      }
    }
  }

  onDestroy(() => {
    disconnect();
  });
</script>

<main>
  <div class="bg-[#330202] w-full h-full h-screen">
    <div class="p-4">
      {#if !isUsernameSet}
        <div class="max-w-md mx-auto mt-20 p-6 bg-white border-[#840404] border-8 rounded-lg shadow-md">
          <h1 class="text-3xl font-bold mb-4">Welcome to Co-Draw</h1>
          <div class="mb-4">
            <label for="username" class="block text-sm font-medium text-gray-700 mb-2">Enter your username:</label>
            <input
              type="text"
              id="username"
              bind:value={username}
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-black"
              placeholder="Your username"
              on:keydown={(e) => e.key === 'Enter' && setUsername()}
            >
          </div>
          <button 
            class="w-full bg-[#840404] hover:bg-red-800 text-white font-bold py-2 px-4 rounded"
            on:click={setUsername}
            disabled={!username.trim()}>
            Join
          </button>
        </div>
      {:else}
        <div class="flex flex-col h-[calc(100vh-2rem)]">
          <div class="flex justify-between items-center mb-4 text-white">
            <h1 class="text-2xl font-bold">Co-Draw Whiteboard</h1>
            <div class="flex items-center gap-4">
              <span class="text-sm px-3 py-1 rounded-full {connectionStatus === 'Connected' ? 'bg-green-700' : 'bg-red-700'}">
                {connectionStatus}
              </span>
              <span class="text-sm">Drawing as: <strong>{username}</strong></span>
            </div>
          </div>
          
          <div class="flex-1 bg-white rounded-lg overflow-hidden shadow-lg">
            <Canvas bind:this={canvasComponent} {username} receivedEvents={messages} />
          </div>
          
          <div class="mt-4 text-white text-sm">
            <p>Connected users will see your drawings in real-time. Try it out!</p>
          </div>
        </div>
      {/if}
    </div>
  </div>
</main>

<style>
  :global(body) {
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen,
      Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  }
</style>