export const fields={nameAr:'الاسم الكامل بالعربية',nameLatin:'الاسم الكامل بالحروف اللاتينية',birthDate:'تاريخ الازدياد',massarId:'رقم مسار',classId:'القسم',ordinalNumber:'الرقم الترتيبي'};
export const statuses={submitted:'طلب جديد',preparing:'مقبول وقيد التجهيز',ready:'جاهز للاستلام',delivered:'تم التسليم',rejected:'مرفوض'};
export const transitions={submitted:['preparing','rejected'],preparing:['ready','rejected'],ready:['delivered'],delivered:[],rejected:[]};
export function checkTransition(from,to,note){if(!transitions[from]?.includes(to))throw Error('انتقال غير مسموح. حدّث اللائحة.');if(note.length>500||(to==='rejected'&&!note.trim()))throw Error('اكتب سبب الرفض (حتى 500 حرف).');}
