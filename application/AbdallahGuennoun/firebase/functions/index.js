const {onDocumentWritten}=require('firebase-functions/v2/firestore');
const {initializeApp}=require('firebase-admin/app');
const {getMessaging}=require('firebase-admin/messaging');
const {getFirestore,FieldValue}=require('firebase-admin/firestore');
const {createHash}=require('node:crypto');
const {publicationMessage}=require('./publication');
initializeApp();
for(const kind of ['news','guidance','timetables']) {
  exports[`${kind}Published`]=onDocumentWritten({document:`${kind}/{id}`,region:'europe-west1',retry:true,maxInstances:2},async event=>{
    const message=publicationMessage(kind,event.data?.before.data(),event.data?.after.data());
    if(!message)return;
    const key=createHash('sha256').update(event.id).digest('hex');
    const record=getFirestore().collection('_notificationDelivery').doc(key);
    const claimed=await getFirestore().runTransaction(async tx=>{
      const current=await tx.get(record);
      if(current.data()?.sent)return false;
      if(current.data()?.leaseUntil>Date.now())throw new Error('Delivery lease is busy');
      tx.set(record,{leaseUntil:Date.now()+120000,createdAt:FieldValue.serverTimestamp()},{merge:true});
      return true;
    });
    if(!claimed)return;
    try {
      await getMessaging().send({topic:'school_publications',notification:message,data:{eventId:key,section:kind},android:{ttl:86400000,notification:{channelId:'school_updates',icon:'ic_school_notification',tag:key}}});
      await record.set({sent:true,sentAt:FieldValue.serverTimestamp(),leaseUntil:0},{merge:true});
    }catch(error){await record.set({leaseUntil:0},{merge:true});throw error;}
  });
}
