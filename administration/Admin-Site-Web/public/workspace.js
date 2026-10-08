// Administrative CRUD. Permissions are also enforced by firestore.rules.
const $=id=>document.getElementById(id);
const stateNames={draft:'مسودة',published:'منشور',archived:'مؤرشف'};
const levels=['TC','1BAC','2BAC'];
const textField=(key,label,extra={})=>({key,label,type:'text',required:true,...extra});
const stateField={key:'status',label:'حالة النشر',type:'select',options:Object.entries(stateNames)};
const urlField=(key,label,required=false)=>textField(key,label,{type:'url',required,maxLength:2048});
const definitions={
 classes:{title:'الأقسام',add:'إضافة قسم',note:'تعديل الاسم والترتيب والتفعيل. لا يتغيّر معرّف القسم؛ أسماء الأقسام في الطلبات السابقة تبقى كما أُرسلت. أوقف القسم بدل حذفه للحفاظ على الملفات.',fields:[textField('name','اسم القسم',{maxLength:50,pattern:'[A-Za-z0-9-]+'}),{key:'levelId',label:'المستوى',type:'select',options:levels.map(x=>[x,x]),immutable:true},textField('academicYear','الموسم الدراسي',{pattern:'[0-9]{4}-[0-9]{4}',immutable:true}),{key:'sortOrder',label:'ترتيب العرض',type:'number',min:1,max:9999,required:true},{key:'active',label:'ظاهر للتلاميذ',type:'checkbox'}]},

};
export function createWorkspace({api,db,auth,message,loadApplications}){
 let active=false,tab='applications',epoch=0,list=[],cursor=null,classes=[],catalog={},editing=null,saving=false;
 const err=e=>e.code==='conflict'?'تم تعديل السجل من مسؤول آخر. أغلق النافذة وحدّث اللائحة قبل المحاولة.':e.code==='permission-denied'?'تعذّر الوصول. تحقق من صلاحية حسابك ونشر قواعد Firebase المرفقة.':e.message||'تعذّر الاتصال. أعد المحاولة.';
 const refFor=(collection,id)=>api.doc(db,collection,id);
 function stop(){active=false;epoch++;list=[];classes=[];catalog={};editing=null;$('catalog-body').replaceChildren();$('editor').close();}
 function start(){active=true;switchTab('applications');}
 function clearBody(){ $('catalog-body').replaceChildren();$('catalog-more').hidden=true; }
 async function switchTab(next){if(!active)return;epoch++;tab=next;list=[];cursor=null;clearBody();$('catalog-search').value='';document.querySelectorAll('[data-tab]').forEach(b=>{if(b.dataset.tab===tab)b.setAttribute('aria-current','page');else b.removeAttribute('aria-current')});$('page-title').textContent=definitions[tab]?.title||(tab==='settings'?'إعدادات المؤسسة':'طلبات التسجيل');$('applications-view').hidden=tab!=='applications';$('workspace').hidden=tab==='applications';if(tab==='applications'){message('');return}$('catalog-add').hidden=tab==='settings';$('search-label').hidden=tab==='settings';$('workspace-note').textContent=definitions[tab]?.note||'إعدادات المؤسسة وحساب الإدارة.';$('catalog-add').textContent=definitions[tab]?.add||'';await load();}
 function button(label,fn,kind=''){const b=document.createElement('button');b.textContent=label;b.className=kind;b.onclick=fn;return b}
 function para(text){const p=document.createElement('p');p.textContent=text;return p}
 function safeLink(url){try{const u=new URL(url);return u.protocol==='https:'&&!u.username&&!u.password}catch{return false}}
 async function load(more=false){if(!active||tab==='applications')return;const token=++epoch,collection=tab;message('جاري التحميل…');$('catalog-refresh').disabled=true;$('catalog-add').disabled=true;$('catalog-more').disabled=true;try{
 const clauses=[api.orderBy(api.documentId()),api.limit(collection==='classes'?500:50)];if(more&&cursor)clauses.push(api.startAfter(cursor));const snap=await api.getDocsFromServer(api.query(api.collection(db,collection),...clauses));if(token!==epoch||!active)return;list=more?list:[];list.push(...snap.docs.map(d=>({...d.data(),id:d.id,_snapshot:d})));cursor=snap.docs.at(-1)||null;$('catalog-more').hidden=snap.size<(collection==='classes'?500:50);render();message('');
 }catch(e){if(token===epoch){clearBody();message(err(e))}}finally{if(token===epoch){$('catalog-refresh').disabled=false;$('catalog-add').disabled=false;$('catalog-more').disabled=false}}}
 function render(){clearBody();$('catalog-more').hidden=!cursor||list.length%(tab==='classes'?500:50)!==0;const needle=$('catalog-search').value.trim().toLowerCase();const filtered=list.filter(r=>(!$('class-level').value||r.levelId===$('class-level').value)).filter(r=>[r.name,r.title,r.levelId,r.className,r.academicYear].join(' ').toLowerCase().includes(needle));if(tab==='classes')filtered.sort((a,b)=>levels.indexOf(a.levelId)-levels.indexOf(b.levelId)||(a.sortOrder||0)-(b.sortOrder||0));$('catalog-body').append(para(`${filtered.length} سجل ظاهر من ${list.length} سجل محمّل`));if(!filtered.length){$('catalog-body').append(para('لا توجد سجلات مطابقة.'));return}const grid=document.createElement('div');grid.className='catalog-grid';for(const r of filtered){const card=document.createElement('article');card.className='panel catalog-card';const badge=document.createElement('span');badge.className='badge';badge.textContent=r.status?stateNames[r.status]:(r.active?'مفعّل':'متوقف');const title=document.createElement('h2');title.textContent=r.name||r.title;card.append(badge,title);card.append(para([r.levelId,r.className,r.subject,r.academicYear].filter(Boolean).join(' · ')));if(r.body)card.append(para(r.body.slice(0,180)+(r.body.length>180?'…':'')));if(r.notes)card.append(para(r.notes));if(tab==='classes')card.append(para(`ترتيب العرض: ${r.sortOrder}`));const url=r.fileUrl||r.url||r.inviteUrl||r.linkUrl;if(url&&safeLink(url)){const a=document.createElement('a');a.href=url;a.target='_blank';a.rel='noopener noreferrer';a.textContent=tab==='rooms'?'فتح Telegram':'فتح الرابط';card.append(a)}const actions=document.createElement('div');actions.className='actions';actions.append(button('تعديل',()=>openEditor(r)));card.append(actions);grid.append(card)}$('catalog-body').append(grid)}
 async function options(){const [c,y]=await Promise.all([api.getDocsFromServer(api.collection(db,'classes')),api.getDocFromServer(refFor('schoolConfig','catalog'))]);classes=c.docs.map(d=>({...d.data(),id:d.id}));catalog=y.exists()?y.data():{}}
 async function openEditor(row=null){const type=tab,token=epoch;try{$('catalog-add').disabled=true;if(['classes','timetables','rooms'].includes(type))await options();if(token!==epoch||!active)return;const def=definitions[type];editing={type,row};$('editor-title').textContent=row?'تعديل '+(row.name||row.title):def.add;$('editor-error').textContent='';$('editor-fields').replaceChildren();for(const field of def.fields)makeField(field,row||{active:true,status:'draft',academicYear:catalog.academicYear||'',sortOrder:1});$('editor').showModal()}catch(e){message(err(e))}finally{$('catalog-add').disabled=false}}
 function makeField(f,values){const label=document.createElement('label');label.textContent=f.label;let input;if(f.type==='select'||f.type==='class'){input=document.createElement('select');let opts=f.options;if(f.type==='class'){opts=classes.filter(c=>c.active||c.id===values.classId).map(c=>[c.id,`${c.name} · ${c.academicYear}`]);if(values.classId&&!opts.some(([id])=>id===values.classId))opts.unshift([values.classId,values.className||values.classId]);opts.unshift(['','اختر القسم'])}for(const [v,t]of opts){const option=document.createElement('option');option.value=v;option.textContent=t;input.append(option)}}else{input=document.createElement(f.type==='textarea'?'textarea':'input');if(f.type!=='textarea')input.type=f.type}input.name=f.key;input.id='edit-'+f.key;if(f.type==='checkbox'){input.checked=values[f.key]===true;label.className='check-label'}else input.value=values[f.key]??'';for(const key of ['required','maxLength','min','max','pattern'])if(f[key]!==undefined)input[key]=f[key];if(f.immutable&&editing.row)input.disabled=true;label.append(input);$('editor-fields').append(label)}
function canonicalValue(value) {
  if (value === null || typeof value !== "object") {
    return value;
  }

  if (Array.isArray(value)) {
    return value.map(canonicalValue);
  }

  return Object.fromEntries(
    Object.keys(value)
      .sort()
      .map(key => [key, canonicalValue(value[key])])
  );
}

function fingerprint(snap) {
  return snap.exists()
    ? JSON.stringify(canonicalValue(snap.data()))
    : null;
}
 async function commit(type,id,data,old){const uid=auth.currentUser?.uid;if(!active||!uid)throw Error('سجّل الدخول مجدداً.');await api.runTransaction(db,async t=>{const ref=refFor(type,id),fresh=await t.get(ref);if(fingerprint(fresh)!==(old?fingerprint(old):null))throw {code:'conflict'};const payload={...data,updatedAt:api.serverTimestamp(),updatedBy:uid};if(!fresh.exists()){payload.createdAt=api.serverTimestamp();payload.createdBy=uid}t.set(ref,payload,{merge:true})})}
 async function save(e){e.preventDefault();if(saving||!editing||!active)return;const task=editing,token=epoch;const fields=task.fields||definitions[task.type].fields,data={};for(const f of fields){const input=$('edit-'+f.key);data[f.key]=f.type==='checkbox'?input.checked:f.type==='number'?Number(input.value):input.value.trim();if(f.type==='url'&&data[f.key]&&!safeLink(data[f.key])){$('editor-error').textContent='استعمل رابط HTTPS صالحاً.';return}}if(data.name)data.name=data.name.toUpperCase().replace(/\s+/g,'');if(!data.name||!Number.isInteger(data.sortOrder)||data.sortOrder<1||data.sortOrder>9999){$('editor-error').textContent='أدخل اسم القسم وترتيباً صحيحاً بين 1 و9999.';return}if(data.academicYear){const [a,b]=data.academicYear.split('-').map(Number);if(b!==a+1){$('editor-error').textContent='الموسم الدراسي يتكوّن من سنتين متتاليتين.';return}}if(task.type==='rooms'&&!/^https:\/\/t\.me\/[^\s]+$/.test(data.inviteUrl)){$('editor-error').textContent='استعمل رابط دعوة يبدأ بـ https://t.me/';return}if(task.type==='classes'&&classes.some(c=>c.id!==task.row?.id&&c.name.toUpperCase()===data.name&&c.academicYear===data.academicYear)){$('editor-error').textContent='هذا القسم موجود في نفس الموسم.';return}if(['timetables','rooms'].includes(task.type)){const c=classes.find(c=>c.id===data.classId);if(!c){$('editor-error').textContent='القسم غير موجود.';return}data.className=c.name;if(task.type==='timetables'&&data.academicYear!==c.academicYear){$('editor-error').textContent='موسم استعمال الزمن يجب أن يطابق موسم القسم.';return}}
 if(task.type==='classes'&&task.row&&task.row.active&&!data.active&&!confirm('إيقاف القسم سيخفيه من اختيارات التسجيل الجديدة، مع الاحتفاظ بملفات التلاميذ. متابعة؟'))return;
 if(task.id==='catalog'&&!confirm('تغيير الموسم يغيّر الأقسام المتاحة للتسجيل. هل جهّزت أقسام الموسم الجديد؟'))return;
 saving=true;$('editor-save').disabled=true;$('editor-close').disabled=true;$('editor-cancel').disabled=true;
 try{const id=task.id||task.row?.id||(api.doc(api.collection(db,task.type)).id);await commit(task.type,id,data,task.row?._snapshot);if(token!==epoch||!active)return;$('editor').close();await load();message('تم حفظ التغييرات.')}catch(e){if(active)$('editor-error').textContent=err(e)}finally{saving=false;$('editor-save').disabled=false;$('editor-close').disabled=false;$('editor-cancel').disabled=false}}
 document.querySelectorAll('[data-tab]').forEach(b=>b.onclick=()=>switchTab(b.dataset.tab));$('catalog-add').onclick=()=>openEditor();$('catalog-refresh').onclick=()=>load();$('catalog-more').onclick=()=>load(true);$('catalog-search').oninput=render;$('class-level').onchange=render;$('editor-form').onsubmit=save;for(const id of ['editor-close','editor-cancel'])$(id).onclick=()=>{if(!saving)$('editor').close()};$('editor').addEventListener('cancel',e=>{if(saving)e.preventDefault()});return {start,stop};
}
