const labels = {news:'أخبار المؤسسة',guidance:'التوجيه المدرسي',timetables:'استعمال الزمن'};
function publicationMessage(kind,before,after) {
  if(!labels[kind] || !after || after.status !== 'published' || before?.status === 'published') return null;
  return {title:labels[kind],body:'نشرت الإدارة محتوى جديداً. افتح التطبيق للاطلاع عليه.'};
}
module.exports={publicationMessage};
