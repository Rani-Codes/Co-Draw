<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { connect, disconnect, sendDrawEvent, type DrawEvent } from './services/websocket';

  let connectionStatus = 'Disconnected';
  let messages: DrawEvent[] = [];
  let username = 'User-' + Math.floor(Math.random() * 1000);

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

  onMount(async () => {
    try {
      connectionStatus = 'Connecting...';
      await connect(handleMessage);
      connectionStatus = 'Connected';
    } catch (error) {
      connectionStatus = `Connection failed: ${error}`;
      console.error('WebSocket connection error:', error);
    }
  });

  onDestroy(() => {
    disconnect();
  });
</script>

<main>
  <div class="bg-red-100 w-full h-full h-screen">
    <div class="p-4">
      <h1 class="text-3xl">WebSocket Status: {connectionStatus}</h1>
      <p class="text-xl">Connected as: {username}</p>
      
      <button 
        class="mt-4 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
        on:click={sendTestMessage}
        disabled={connectionStatus !== 'Connected'}>
        Send Test Message
      </button>
      
      {#if messages.length > 0}
        <div class="mt-4">
          <h2 class="text-2xl">Received Messages:</h2>
          <div class="bg-white p-4 rounded mt-2 max-h-60 overflow-y-auto">
            {#each messages as message, i}
              <div class="mb-2 p-2 bg-blue-50 rounded">
                <p><span class="font-bold">{message.username}</span>: {message.type} event at ({message.x.toFixed(2)}, {message.y.toFixed(2)})</p>
              </div>
            {/each}
          </div>
        </div>
      {/if}
    </div>
  </div>
</main>
