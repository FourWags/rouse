// Offline contract: every dog choice has three coats and both alarm poses.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const assets=path.join(__dirname,'../app/src/main/assets');
const rows=JSON.parse(fs.readFileSync(path.join(assets,'dog-themes.json'),'utf8'));
assert.equal(rows.length,240);assert.equal(new Set(rows.map(x=>x.id)).size,240);
assert.equal(new Set(rows.filter(x=>x.kind==='breed').map(x=>x.breed)).size,50);
assert.equal(new Set(rows.filter(x=>x.kind==='mix').map(x=>x.breed)).size,30);
assert(rows.some(row=>row.breed==='Goldendoodle'));
assert(!rows.some(row=>row.breed==='Poodle'));
for(const breed of new Set(rows.map(row=>row.breed))){
 const choices=rows.filter(row=>row.breed===breed);
 assert(choices.length>=3,`${breed} has fewer than three coat choices`);
 assert.equal(new Set(choices.map(row=>[row.bg,row.action,row.accent].join('/'))).size,choices.length,`${breed} reuses a color theme`);
}
const coatCss=fs.readFileSync(path.join(assets,'dog-coats.css'),'utf8');
for(const row of rows){
 assert(/^dog_[a-z0-9_]+$/.test(row.id));
 assert(coatCss.includes(`.coat-swatch[data-coat="${row.id}"]{--coat-bg:${row.bg};--coat-action:${row.action};--coat-accent:${row.accent}}`));
 assert(fs.existsSync(path.join(assets,'dog-thumbs',`${row.id}.webp`)));
 for(const kind of ['curl','yawn']){
  const file=path.join(assets,'dog-poses',`${row.id}-${kind}.webp`);
  assert(fs.existsSync(file),file);assert(fs.statSync(file).size>1000,file);
 }
}
const elements=new Map(),tokens={};
const get=id=>{if(!elements.has(id))elements.set(id,{value:'',innerHTML:'',textContent:'',style:{},dataset:{},setAttribute(){},removeAttribute(){},querySelector(){return {style:{}}}});return elements.get(id)};
const context=vm.createContext({window:{},location:{hash:'#dog_shiba_0/wr_s2'},document:{getElementById:get,querySelectorAll:()=>[],documentElement:{dataset:{},style:{setProperty:(k,v)=>tokens[k]=v,removeProperty:k=>delete tokens[k]}}}});
for(const file of ['appearance.js','dog-themes.js','icon-templates.js','custom-themes.js'])vm.runInContext(fs.readFileSync(path.join(assets,file),'utf8'),context);
assert.equal(JSON.stringify(rows),JSON.stringify(context.window.DOG_THEMES));
const app=fs.readFileSync(path.join(assets,'app.js'),'utf8');vm.runInContext(app.slice(0,app.indexOf('renderAppearanceOptions();applyAppearance')),context);
vm.runInContext('renderAppearanceOptions();st={settings:{dogCompanion:"dog_shiba_0"}};applyAppearance("dog_shiba_0","wr_s2")',context);
assert.equal(get('themeSelect').value,'');
assert(!get('themeSelect').innerHTML.includes('dog_shiba_0'));
assert.equal(vm.runInContext('dogBreeds[0]',context),'Shiba Inu');
assert.equal(vm.runInContext('dogBreeds[1]',context),'French Bulldog');
assert.equal((get('dogBreed').innerHTML.match(/<option /g)||[]).length,81);
assert(get('dogBreed').innerHTML.includes('Named dogs · 50'));
assert(get('dogBreed').innerHTML.includes('Mixed companions · 30'));
assert.equal(get('dogBreed').value,'Shiba Inu');
assert.equal(get('dogSnoozePreview').src,'dog-poses/dog_shiba_0-curl.webp');
assert.equal(get('dogAwakePreview').src,'dog-poses/dog_shiba_0-yawn.webp');
assert.equal((get('dogOptions').innerHTML.match(/class="dog-option"/g)||[]).length,3);
assert(!get('dogOptions').innerHTML.includes('style='));
assert(get('dogOptions').innerHTML.includes('data-dog="dog_shiba_0" aria-pressed="true"'));
assert.equal((get('themeOptions').innerHTML.match(/class="theme-option"/g)||[]).length,19);
for(const row of rows){
 vm.runInContext(`st={settings:{dogCompanion:${JSON.stringify(row.id)}}};applyAppearance(${JSON.stringify(row.id)},'wr_s2')`,context);
 assert.equal(get('themeSelect').value,'');assert.equal(tokens['--paper'],row.bg);
 assert(get('dogOptions').innerHTML.includes(`data-dog="${row.id}" aria-pressed="true"`));
 assert.equal(get('dogBreed').value,row.breed);
 assert.equal(get('dogSnoozePreview').src,`dog-poses/${row.id}-curl.webp`);
}
vm.runInContext("dogPreviewId='dog_husky_0';renderDogOptions()",context);
assert.equal((get('dogOptions').innerHTML.match(/class="dog-option"/g)||[]).length,3);
get('dogSearch').value='pomeranian';vm.runInContext('renderDogOptions()',context);
assert(get('dogBreed').innerHTML.includes('Pomeranian'));
assert(!get('dogBreed').innerHTML.includes('Golden Retriever'));
console.log('PASS: 50 named dogs, 30 mutts, 240 coat choices, 480 offline poses, searchable picker, saved selection UI');
