#!/usr/bin/env python3
"""Render the Java sequence trace as a self-contained offline playback page."""
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
directory = ROOT / "build/software-checks"
rows = list(csv.DictReader((directory / "trace.csv").open()))
trace = [{"t": float(r["seconds"]), "state": r["state"], "x": float(r["x"]),
          "y": float(r["y"]), "h": float(r["headingDegrees"]),
          "stopped": r["outputsStopped"] == "true"} for r in rows]
pause = [r for r in trace if r["state"] == "SHOOTING_PAUSE"]
assert pause and all(r["stopped"] for r in pause), "Pause must have zero output requests"
html = r'''<!doctype html>
<html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Morpheus — Java sequence preview</title>
<style>
*{box-sizing:border-box}body{margin:0;background:#101823;color:#e8f0f7;font:16px system-ui,sans-serif}
main{max-width:1100px;margin:auto;padding:32px}h1{margin:0 0 8px;font-size:30px}p{color:#b8c9da;line-height:1.55}
.layout{display:grid;grid-template-columns:minmax(300px,650px) 1fr;gap:28px}.panel{background:#1a2838;padding:22px;border-radius:12px}
svg{width:100%;background:#253548;border-radius:10px}.status{font-size:22px;font-weight:700;color:#7dd3fc;overflow-wrap:anywhere}
button,input{font:inherit}button{padding:10px 16px;margin-right:8px;border:0;border-radius:6px;background:#7dd3fc;color:#101823;cursor:pointer}
input{width:100%;margin:20px 0}a{color:#7dd3fc}small{color:#b8c9da}.badge{color:#fcd34d}dl{line-height:1.7}dd{margin-left:0;font-variant-numeric:tabular-nums}
@media(max-width:760px){main{padding:16px}.layout{grid-template-columns:1fr}}
</style><main><h1>Morpheus · drive, pause, park</h1>
<p>Playback of the production Java sequence and Pedro 3 path geometry with ideal synthetic motion.
<strong>No FTC OpMode, Pedro follower controller, sensor model, or robot hardware runs here.</strong></p>
<div class="layout"><div><svg id="field" viewBox="-8 -8 157.5 162" role="img" aria-label="Audience-bottom BIOBUZZ schematic and robot route">
<g transform="translate(0 141.5) scale(1 -1)"><rect width="141.5" height="141.5" fill="#27394b" stroke="#b8c9da" stroke-width=".6"/>
<g id="grid" stroke="#43586c" stroke-width=".4"></g>
<g fill="#b42d45" opacity=".7"><rect x="0" y="94.75" width="11" height="24"/><rect x="46" y="50" width="3" height="41.4"/></g>
<g fill="#2563eb" opacity=".7"><rect x="130.5" y="22.75" width="11" height="24"/><rect x="93.9" y="50" width="3" height="41.4"/></g>
<g id="routes" fill="none" stroke-width="1"></g><g id="bot"><rect x="-8" y="-8" width="16" height="16" rx="1" fill="#e8f0f7" opacity=".8"/>
<path d="M0 0 L11 0 M8 -2 L11 0 L8 2" fill="none" stroke="#101823" stroke-width="1"/></g></g>
<text x="45" y="152" fill="#b8c9da" font-size="4">AUDIENCE · +x → · +y ↑</text></svg>
<small>Schematic only. HIVE bars and zones are approximate; field elements, heights and swept collisions are incomplete. Robot box is an unmeasured 16 × 16 inch example.</small></div>
<div class="panel"><span class="badge">SOFTWARE PREVIEW</span><h2 id="state" class="status"></h2>
<dl><dt>Elapsed</dt><dd id="time"></dd><dt>Pose · inches, degrees</dt><dd id="pose"></dd><dt>Drive outputs requested</dt><dd id="outputs"></dd></dl>
<button id="play">Play</button><button id="reset">Reset</button><label for="scrub"><input id="scrub" type="range" min="0" step="1" value="0">Timeline</label>
<p>The shooting pause is part of the Java state machine. This page replays its recorded states; it does not reimplement the sequence.</p>
<p>Edit the <a href="https://visualizer.pedropathing.com/" target="_blank" rel="noopener">official Pedro visualizer</a> project, regenerate Java, and rerun the desktop checks to update this preview.</p></div></div></main>
<script>
const trace=__TRACE__; const svgNS='http://www.w3.org/2000/svg';
const scrub=document.getElementById('scrub');scrub.max=trace.length-1;
for(let n=1;n<6;n++){for(const [a,b,c,d] of [[n*24-1.25,0,n*24-1.25,141.5],[0,n*24-1.25,141.5,n*24-1.25]]){
 const el=document.createElementNS(svgNS,'line');for(const [k,v]of Object.entries({x1:a,y1:b,x2:c,y2:d}))el.setAttribute(k,v);document.getElementById('grid').append(el);}}
for(const [state,color] of [['TO_SHOOT','#38bdf8'],['TO_PARK','#4ade80']]){
 const el=document.createElementNS(svgNS,'polyline');el.setAttribute('points',trace.filter(r=>r.state===state).map(r=>`${r.x},${r.y}`).join(' '));el.setAttribute('stroke',color);document.getElementById('routes').append(el);}
let playing=false,index=0,last=0,elapsed=0;
function render(){const r=trace[index];scrub.value=index;
document.getElementById('state').textContent=r.state.replaceAll('_',' ');
document.getElementById('time').textContent=r.t.toFixed(2)+' s';document.getElementById('pose').textContent=`(${r.x.toFixed(2)}, ${r.y.toFixed(2)}, ${r.h.toFixed(1)}°)`;
document.getElementById('outputs').textContent=r.stopped?'ZERO / STOPPED':'IDEAL MOTION';document.getElementById('bot').setAttribute('transform',`translate(${r.x} ${r.y}) rotate(${r.h})`);}
document.getElementById('play').onclick=()=>{if(index===trace.length-1){index=0;elapsed=0;}playing=!playing;last=0;document.getElementById('play').textContent=playing?'Pause':'Play';};
document.getElementById('reset').onclick=()=>{playing=false;index=0;elapsed=0;last=0;document.getElementById('play').textContent='Play';render();};
scrub.oninput=()=>{index=Number(scrub.value);elapsed=trace[index].t;render();};
function frame(now){if(playing){if(last)elapsed+=(now-last)/1000;last=now;while(index<trace.length-1&&trace[index+1].t<=elapsed)index++;render();if(index===trace.length-1){playing=false;document.getElementById('play').textContent='Play';}}requestAnimationFrame(frame);}render();requestAnimationFrame(frame);
</script></html>'''
output = directory / "preview.html"
output.write_text(html.replace("__TRACE__", json.dumps(trace)))
print(f"Offline sequence preview: {output}")
