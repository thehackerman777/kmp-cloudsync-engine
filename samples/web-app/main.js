/**
 * CloudSync Engine - Web Sample
 * Uses the UMD bundle (engine.js) loaded via <script> tag.
 * CloudSyncEngine is available as a global.
 */

let engine = null;

function log(message) {
    const logEl = document.getElementById('log');
    const timestamp = new Date().toLocaleTimeString();
    logEl.textContent = `[${timestamp}] ${message}\n` + logEl.textContent.substring(0, 2000);
}

function updateStatus(text) {
    document.getElementById('status').textContent = text;
}

function startEngine() {
    try {
        log('Creating engine instance...');
        engine = CloudSyncEngine.create('{"configName":"web-sample","serverUrl":"https://api.example.com"}');
        log('Starting engine...');
        engine.start();
        const state = engine.getState();
        updateStatus('Started - State: ' + state);
        log('Engine started! State: ' + state);
    } catch (e) {
        log('ERROR: ' + e.message);
        updateStatus('Error: ' + e.message);
    }
}

function syncNow() {
    if (!engine) {
        log('ERROR: Engine not started. Click Start first.');
        return;
    }
    try {
        log('Triggering sync...');
        engine.syncNow();
        const state = engine.getState();
        updateStatus('Syncing - State: ' + state);
        log('Sync triggered! State: ' + state);
    } catch (e) {
        log('ERROR: ' + e.message);
    }
}

function stopEngine() {
    if (!engine) {
        log('ERROR: Engine not started.');
        return;
    }
    try {
        log('Stopping engine...');
        engine.stop();
        updateStatus('Stopped');
        log('Engine stopped!');
        engine = null;
    } catch (e) {
        log('ERROR: ' + e.message);
    }
}

log('Web sample loaded. Click "Start Engine" to begin.');
