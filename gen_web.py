#!/usr/bin/env python3
import os

html = r"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>CloudSync Engine - Web Demo</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;background:#0f172a;color:#e2e8f0}
.container{max-width:900px;margin:0 auto;padding:24px}
h1{font-size:1.8rem;font-weight:700;margin-bottom:4px}
.sub{color:#94a3b8;font-size:.9rem;margin-bottom:24px}
.sub a{color:#38bdf8}
.card{background:#1e293b;border-radius:12px;padding:20px;margin-bottom:16px;border:1px solid #334155}
.card h2{font-size:1rem;color:#38bdf8;margin-bottom:12px;text-transform:uppercase;letter-spacing:.5px}
.st{display:inline-block;padding:4px 12px;border-radius:20px;font-size:.85rem;font-weight:600}
.st.idle{background:#334155;color:#94a3b8}
.st.ready{background:#166534;color:#86efac}
.st.sync{background:#1e40af;color:#93c5fd}
.st.err{background:#7f1d1d;color:#fca5a5}
.grp{display:flex;gap:8px;flex-wrap:wrap}
.btn{padding:10px 20px;border:none;border-radius:8px;font-size:.9rem;font-weight:600;cursor:pointer}
.btn:disabled{opacity:.4;cursor:not-allowed}
.btn.pri{background:#2563eb;color:white}
.btn.pri:hover:not(:disabled){background:#1d4ed8}
.btn.suc{background:#16a34a;color:white}
.btn.suc:hover:not(:disabled){background:#15803d}
.btn.dan{background:#dc2626;color:white}
.btn.dan:hover:not(:disabled){background:#b91c1c}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:12px}
.grid label{color:#94a3b8;font-size:.8rem;display:block}
.grid val{font-size:.9rem;font-weight:500}
.log-box{background:#0f172a;border-radius:8px;padding:12px;font-family:"Fira Code",monospace;font-size:.8rem;max-height:300px;overflow-y:auto;white-space:pre-wrap;line-height:1.5}
.l{padding:2px 0;border-bottom:1px solid #1e293b}
.bdg{display:inline-block;padding:2px 8px;border-radius:4px;font-size:.7rem;font-weight:700;margin-right:4px}
.bdg.ok{background:#166534;color:#86efac}
.bdg.info{background:#1e40af;color:#93c5fd}
.bdg.err{background:#7f1d1d;color:#fca5a5}
.bdg.warn{background:#854d0e;color:#fde68a}
</style>
</head>
<body>
<div class="container">
<h1>&#x2601;&#xFE0F; CloudSync Engine</h1>
<p class="sub">Web Demo &mdash; Mode: <strong>MOCK</strong> (no Google credentials) &middot; <a href="https://github.com/thehackerman777/kmp-cloudsync-engine">GitHub</a></p>
<div class="card">
<h2>&#x1F4E1; Engine Status</h2>
<p id="statusDisplay"><span class="st idle">&#x26AA; Not initialized</span></p>
<div class="grid">
<div><label>State</label><val id="stateVal">&mdash;</val></div>
<div><label>User ID</label><val id="userIdVal">&mdash;</val></div>
<div><label>Provider</label><val id="provVal">&mdash;</val></div>
<div><label>Auth</label><val id="authVal">&mdash;</val></div>
</div>
</div>
<div class="card">
<h2>&#x1F3AE; Controls</h2>
<div class="grp">
<button class="btn pri" id="btnInit" onclick="initEngine()">&#x1F680; Init + Mock Auth</button>
<button class="btn suc" id="btnSync" onclick="syncNow()" disabled>&#x1F504; Sync Now</button>
<button class="btn dan" id="btnStop" onclick="stopEngine()" disabled>&#x23F9; Stop</button>
</div>
</div>
<div class="card">
<h2>&#x1F4CB; Configurations</h2>
<div id="configList" style="color:#94a3b8;font-size:0.9rem">Initialize engine to see configs.</div>
</div>
<div class="card">
<h2>&#x1F4DC; Activity Log</h2>
<div class="log-box" id="log"><div class="l">Web demo loaded. Click Init to start.</div></div>
</div>
</div>
<script src="engine.js"></script>
<script>
var engine=null;
function log(m,t){
 var e=document.getElementById("log"),time=new Date().toLocaleTimeString();
 var b=t=="ok"?'<span class="bdg ok">&#x2705;</span>':t=="err"?'<span class="bdg err">&#x274C;</span>':t=="warn"?'<span class="bdg warn">&#x26A0;</span>':'<span class="bdg info">&#x2139;</span>';
 e.innerHTML='<div class="l">['+time+'] '+b+' '+m+'</div>'+e.innerHTML;
}
function upStat(text,cls){
 document.getElementById("statusDisplay").innerHTML='<span class="st '+cls+'">'+text+'</span>';
}
function upInfo(state,uid,prov,auth){
 document.getElementById("stateVal").textContent=state||'\u2014';
 document.getElementById("userIdVal").textContent=uid?uid.substring(0,16)+'...':'\u2014';
 document.getElementById("provVal").textContent=prov||'\u2014';
 document.getElementById("authVal").textContent=auth||'\u2014';
}
function btns(init,sync,stop){
 document.getElementById("btnInit").disabled=!init;
 document.getElementById("btnSync").disabled=!sync;
 document.getElementById("btnStop").disabled=!stop;
}
function showCfgs(c){
 var e=document.getElementById("configList");
 if(!c||!c.length){e.innerHTML='<span style="color:#94a3b8">No configs.</span>';return;}
 e.innerHTML=c.map(function(x){
  var i=x.synced?'&#x2705;':'&#x23F3;';
  return '<div style="padding:6px 0;border-bottom:1px solid #334155;display:flex;justify-content:space-between"><span>'+i+' '+x.id+' <span style="color:#64748b">v'+x.version+'</span></span><span style="color:#94a3b8;font-size:0.85rem">'+x.namespace+'</span></div>';
 }).join('');
}
async function initEngine(){
 log('Initializing with MOCK mode...','info');
 upStat('Initializing...','sync');
 btns(false,false,false);
 try{
  var cfg=JSON.stringify({configName:'web-demo',serverUrl:'https://www.googleapis.com',mode:'mock'});
  engine=CloudSyncEngine.configure(cfg);
  engine.initialize(cfg);
  log('Engine initialized.','ok');
  upStat('Initialized (MOCK)','ready');
  upInfo('IDLE','mock-user-web','MOCK (Google Drive)','Mock Auth');
  btns(false,true,true);
  log('Ready! No Google credentials needed.','ok');
  showCfgs([{id:'pref-theme',namespace:'app-settings',version:1,synced:false},{id:'pref-lang',namespace:'app-settings',version:1,synced:false},{id:'backup-db',namespace:'data',version:3,synced:true}]);
 }catch(e){
  log('Error: '+e.message,'err');
  upStat('Error: '+e.message,'err');
  btns(true,false,false);
 }
}
async function syncNow(){
 if(!engine){log('Not initialized!','err');return;}
 log('Syncing...','info');
 upStat('Syncing...','sync');
 btns(false,false,true);
 try{
  engine.syncNow();
  log('Sync completed (MOCK mode).','ok');
  upStat('Synced','ready');
  showCfgs([{id:'pref-theme',namespace:'app-settings',version:2,synced:true},{id:'pref-lang',namespace:'app-settings',version:2,synced:true},{id:'backup-db',namespace:'data',version:3,synced:true}]);
  btns(false,true,true);
 }catch(e){
  log('Error: '+e.message,'err');
  upStat('Sync failed','err');
  btns(false,true,true);
 }
}
async function stopEngine(){
 if(!engine)return;
 engine.stop();
 engine=null;
 log('Engine stopped.','ok');
 upStat('Stopped','idle');
 upInfo('\u2014','\u2014','\u2014','\u2014');
 btns(true,false,false);
}
log('Web demo loaded.','info');
</script>
</body>
</html>"""

with open("docs/web-sample/index.html", "w") as f:
    f.write(html)
print(f"Created index.html ({os.path.getsize('docs/web-sample/index.html') // 1024} KB)")
