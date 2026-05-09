# Building with Web (JS Bundle)

## Building the JS Bundle

```bash
cd kmp-cloudsync-engine
./gradlew :engine:jsWebBundle --no-daemon
```

Output: `engine/build/outputs/js/engine.js`

## Using in HTML

```html
<script src="engine.js"></script>
<script>
    // CloudSyncEngine is available globally
    const engine = CloudSyncEngine.create('{"configName":"web-app","serverUrl":"https://api.example.com"}');
    engine.start();
    console.log('State:', engine.getState());
</script>
```

## API Reference (JavaScript)

### `CloudSyncEngine.create(config: string)`
Creates a new engine instance.

### `engine.start()`
Starts the engine asynchronously.

### `engine.stop()`
Stops the engine.

### `engine.syncNow()`
Triggers a sync operation.

### `engine.getState(): string`
Returns current state: `IDLE`, `INITIALIZING`, `STARTING`, `RUNNING`, `SYNCING`, `STOPPING`, `ERROR`

### `engine.reset()`
Resets the engine state.

## Node.js Testing

```bash
cd samples/web-app
npm install
node -e "
    const engine = require('./engine.js');
    // Or load via script tag in a browser context
"
```

## Requirements

- ECMAScript 6+ runtime
- Browser or Node.js 18+
