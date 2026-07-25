export const SCORE_FIELDS = [
  { key: 'waterQuality', label: '水质', hint: '观察颜色、气味、水垢与水压' },
  { key: 'spaceFeeling', label: '空间感', hint: '结合实际面积、层高和动线' },
  { key: 'outlets', label: '插座与排插', hint: '数量、位置、接地及是否老化' },
  { key: 'noise', label: '安静程度', hint: '关窗与开窗分别听 30 秒' },
  { key: 'daylight', label: '采光', hint: '留意遮挡、窗户大小与日照时段' },
  { key: 'ventilation', label: '通风', hint: '是否能形成对流，有无闷热感' },
  { key: 'humidity', label: '干燥防潮', hint: '墙角、衣柜、窗边是否霉变' },
  { key: 'kitchen', label: '厨房', hint: '操作台、油烟、燃气与收纳' },
  { key: 'bathroom', label: '卫浴', hint: '干湿分离、排水、反味与热水' },
  { key: 'storage', label: '收纳', hint: '衣柜、鞋柜及杂物空间' },
  { key: 'signal', label: '网络与信号', hint: '手机信号、宽带选择与网口' },
  { key: 'security', label: '安全性', hint: '门锁、门禁、消防与逃生条件' },
  { key: 'commute', label: '通勤便利', hint: '到地铁/公交及目的地的真实耗时' },
  { key: 'amenities', label: '周边配套', hint: '超市、外卖、医院、快递与夜间照明' },
  { key: 'privacy', label: '隐私', hint: '窗户对视、隔音和公共区域暴露程度' },
  { key: 'management', label: '物业与公共区', hint: '楼道、电梯、垃圾和维修响应' },
  { key: 'cleanliness', label: '卫生与虫害', hint: '异味、蟑螂、鼠迹和下水口' },
];

export const SCORE_TEXT = {
  1: '很差',
  2: '较差',
  3: '一般',
  4: '不错',
  5: '很好',
};

export const emptyRoom = () => {
  const now = new Date().toISOString();
  return {
    id: crypto.randomUUID(),
    title: '',
    status: 'draft',
    rent: '',
    deposit: '',
    managementFee: '',
    otherMonthlyCost: '',
    paymentMethod: '',
    layout: '',
    area: '',
    floor: '',
    totalFloors: '',
    elevator: '',
    moveInDate: '',
    leaseTerm: '',
    agencyFee: '',
    address: '',
    latitude: null,
    longitude: null,
    locationAccuracy: null,
    contactName: '',
    contactPhone: '',
    contactType: '',
    contactNote: '',
    photos: [],
    direction: '',
    directionDegree: null,
    balcony: '',
    acEnergy: '',
    washingMachine: '',
    heating: '',
    cooking: '',
    pets: '',
    parking: '',
    utilityBilling: '',
    scores: {},
    scoreNotes: {},
    pros: '',
    cons: '',
    questions: '',
    overallNote: '',
    tags: [],
    createdAt: now,
    updatedAt: now,
  };
};

export function getAverageScore(room) {
  const values = Object.values(room.scores || {}).filter((value) => Number(value) > 0);
  if (!values.length) return null;
  return values.reduce((sum, value) => sum + Number(value), 0) / values.length;
}

export function getCompletion(room) {
  const basic = [room.rent, room.address, room.contactName, room.area, room.direction];
  const scores = Object.values(room.scores || {});
  const extras = [room.balcony, room.acEnergy, room.washingMachine, room.pros, room.cons];
  const filled = [...basic, ...scores, ...extras].filter((value) => value !== '' && value !== null && value !== undefined).length;
  const total = basic.length + SCORE_FIELDS.length + extras.length;
  return Math.round((filled / total) * 100);
}
