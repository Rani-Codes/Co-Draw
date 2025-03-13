<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { connect, disconnect, sendDrawEvent, type DrawEvent } from './services/websocket';

  let connectionStatus = 'Disconnected';
  let messages: DrawEvent[] = [];
  let username = '';
  let isUsernameSet = false;

  // Handle received messages
  function handleMessage(message: DrawEvent) {
    messages = [...messages, message];
    console.log('Received message:', message);
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
        await connect(handleMessage);
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
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
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
      <div class="text-white">
        <h1 class="text-3xl">WebSocket Status: {connectionStatus}</h1>
        <p class="text-xl">Connected as: {username}</p>
        
        <button 
          class="mt-4 bg-[#840404] hover:bg-red-800 text-white font-bold py-2 px-4 rounded"
          on:click={sendTestMessage}
          disabled={connectionStatus !== 'Connected'}>
          Send Test Message
        </button>

        {#if messages.length > 0}
        <div class="mt-4">
          <h2 class="text-2xl">Received Messages:</h2>
          <div class="bg-white p-4 rounded mt-2 max-h-60 overflow-y-auto">
            {#each messages as message, i}
              <div class="mb-2 p-2 bg-blue-50 rounded text-black">
                <p><span class="font-bold">{message.username}</span>: {message.type} event at ({message.x.toFixed(2)}, {message.y.toFixed(2)})</p>
              </div>
            {/each}
          </div>
        </div>
      {/if}
      </div>
        
      {/if}
    </div>
  </div>
</main>