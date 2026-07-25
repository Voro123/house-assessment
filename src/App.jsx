import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  Building2,
  Camera,
  Check,
  ChevronDown,
  ChevronRight,
  CircleDollarSign,
  Clock3,
  Compass,
  Download,
  FilePlus2,
  Filter,
  ImagePlus,
  LocateFixed,
  MapPin,
  MoreVertical,
  Pencil,
  Plus,
  Save,
  Search,
  ShieldCheck,
  Sparkles,
  Trash2,
  Upload,
  X,
} from 'lucide-react';
import { MapContainer, Marker, TileLayer, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import { deleteRoom, getAllRooms, replaceAllRooms, saveRoom } from './storage.js';
import { emptyRoom, getAverageScore, getCompletion, SCORE_FIELDS, SCORE_TEXT } from './model.js';

const markerIcon = L.divIcon({
  className: 'custom-map-marker',
  html: '<div class="marker-pin"></div>',
  iconSize: [32, 40],
  iconAnchor: [16, 40],
});

function formatDate(value) {
  if (!value) return '—';
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function formatCurrency(value) {
  if (value === '' || value === null || value === undefined) return '租金待补';
  const number = Number(value);
  return Number.isFinite(number) ? `¥${number.toLocaleString('zh-CN')}/月` : `${value}/月`;
}

function cardinalFromDegree(degree) {
  if (degree === null || degree === undefined || Number.isNaN(Number(degree))) return '';
  const directions = ['北', '东北', '东', '东南', '南', '西南', '西', '西北'];
  return directions[Math.round((((Number(degree) % 360) + 360) % 360) / 45) % 8];
}

async function reverseGeocode(latitude, longitude) {
  const response = await fetch(
    `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}&accept-language=zh-CN`,
    { headers: { 'Accept-Language': 'zh-CN' } },
  );
  if (!response.ok) throw new Error('地址解析失败');
  const result = await response.json();
  return result.display_name || '';
}

function MapClickHandler({ onSelect }) {
  useMapEvents({
    click(event) {
      onSelect(event.latlng.lat, event.latlng.lng);
    },
  });
  return null;
}

function MapPicker({ initialPosition, onClose, onConfirm }) {
  const [position, setPosition] = useState(initialPosition || [35.6812, 139.7671]);
  const [address, setAddress] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSelect(lat, lng) {
    setPosition([lat, lng]);
    setLoading(true);
    try {
      setAddress(await reverseGeocode(lat, lng));
    } catch {
      setAddress(`${lat.toFixed(6)}, ${lng.toFixed(6)}`);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="map-modal">
        <div className="modal-header">
          <div>
            <strong>在地图上选位置</strong>
            <span>轻点地图移动标记，确认后仍可手动改地址</span>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="关闭地图"><X size={20} /></button>
        </div>
        <div className="map-wrap">
          <MapContainer center={position} zoom={16} scrollWheelZoom className="map-container">
            <TileLayer
              attribution='&copy; OpenStreetMap contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <Marker position={position} icon={markerIcon} />
            <MapClickHandler onSelect={handleSelect} />
          </MapContainer>
        </div>
        <div className="map-footer">
          <div className="picked-address">
            <MapPin size={18} />
            <span>{loading ? '正在识别地址…' : address || '点击地图选择具体位置'}</span>
          </div>
          <button
            className="primary-button"
            onClick={() => onConfirm({ latitude: position[0], longitude: position[1], address })}
          >
            使用这个位置
          </button>
        </div>
      </div>
    </div>
  );
}

function EmptyState({ onCreate }) {
  return (
    <section className="empty-state">
      <div className="empty-illustration">
        <div className="mini-building one" />
        <div className="mini-building two" />
        <div className="mini-building three" />
        <MapPin size={34} strokeWidth={1.8} />
      </div>
      <h2>第一套房，从现场开始记</h2>
      <p>不用一次填完。先拍照、记租金和位置，其他项目边走边看，离开后也能继续补。</p>
      <button className="primary-button large" onClick={onCreate}><Plus size={19} /> 新建看房记录</button>
    </section>
  );
}

function RoomCard({ room, onEdit, onDelete }) {
  const score = getAverageScore(room);
  const completion = getCompletion(room);
  const cover = room.photos?.[0]?.dataUrl;
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <article className="room-card" onClick={() => onEdit(room)}>
      <div className="room-cover" style={cover ? { backgroundImage: `url(${cover})` } : undefined}>
        {!cover && <Building2 size={38} strokeWidth={1.5} />}
        <span className={`status-pill ${room.status}`}>{room.status === 'complete' ? '已完成' : '记录中'}</span>
        <button
          className="card-menu-button"
          onClick={(event) => { event.stopPropagation(); setMenuOpen((value) => !value); }}
          aria-label="更多操作"
        ><MoreVertical size={18} /></button>
        {menuOpen && (
          <div className="card-menu" onClick={(event) => event.stopPropagation()}>
            <button onClick={() => onEdit(room)}><Pencil size={16} /> 编辑</button>
            <button className="danger" onClick={() => onDelete(room)}><Trash2 size={16} /> 删除</button>
          </div>
        )}
      </div>
      <div className="room-card-body">
        <div className="room-card-title-row">
          <div>
            <h3>{room.title || room.address?.split(',')[0] || '未命名房源'}</h3>
            <p><MapPin size={14} /> {room.address || '地址待补充'}</p>
          </div>
          {score ? <div className="score-badge"><strong>{score.toFixed(1)}</strong><span>/ 5</span></div> : <div className="score-badge empty">未评分</div>}
        </div>
        <div className="room-facts">
          <strong>{formatCurrency(room.rent)}</strong>
          <span>{room.area ? `${room.area}㎡` : '面积待补'}</span>
          <span>{room.layout || '户型待补'}</span>
          <span>{room.direction ? `${room.direction}向` : '朝向待补'}</span>
        </div>
        <div className="progress-row">
          <div className="progress-track"><span style={{ width: `${completion}%` }} /></div>
          <span>{completion}% 已记录</span>
        </div>
        <div className="room-card-footer">
          <span><Clock3 size={14} /> 更新于 {formatDate(room.updatedAt)}</span>
          <span className="open-detail">继续记录 <ChevronRight size={16} /></span>
        </div>
      </div>
    </article>
  );
}

function Dashboard({ rooms, onCreate, onEdit, onDelete, onImport }) {
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const fileRef = useRef(null);

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return rooms
      .filter((room) => filter === 'all' || room.status === filter)
      .filter((room) => !normalized || [room.title, room.address, room.contactName, room.tags?.join(' ')]
        .filter(Boolean).join(' ').toLowerCase().includes(normalized))
      .sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));
  }, [rooms, query, filter]);

  function exportData() {
    const blob = new Blob([JSON.stringify({ version: 1, exportedAt: new Date().toISOString(), rooms }, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `租房评估备份-${new Date().toISOString().slice(0, 10)}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <main className="page-shell dashboard-page">
      <header className="topbar dashboard-topbar">
        <div className="brand">
          <div className="brand-mark"><Building2 size={22} /></div>
          <div><strong>住哪儿</strong><span>租房评估助手</span></div>
        </div>
        <div className="topbar-actions">
          <button className="ghost-button hide-mobile" onClick={exportData}><Download size={17} /> 导出备份</button>
          <button className="ghost-button hide-mobile" onClick={() => fileRef.current?.click()}><Upload size={17} /> 导入</button>
          <button className="primary-button" onClick={onCreate}><Plus size={18} /> 新建记录</button>
          <input ref={fileRef} hidden type="file" accept="application/json" onChange={(event) => onImport(event.target.files?.[0])} />
        </div>
      </header>

      <section className="hero-panel">
        <div>
          <span className="eyebrow"><Sparkles size={15} /> 现场记录，回来再决定</span>
          <h1>别靠回忆选房。<br />把每一处细节带回来。</h1>
          <p>所有项目都允许留空，记录会自动保存。照片、位置、联系人和评分集中在一张看房卡里。</p>
        </div>
        <div className="hero-stats">
          <div><strong>{rooms.length}</strong><span>套房源</span></div>
          <div><strong>{rooms.filter((room) => room.status === 'complete').length}</strong><span>已完成</span></div>
          <div><strong>{rooms.length ? Math.round(rooms.reduce((sum, room) => sum + getCompletion(room), 0) / rooms.length) : 0}%</strong><span>平均完整度</span></div>
        </div>
      </section>

      {rooms.length > 0 && (
        <section className="toolbar">
          <label className="search-box"><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索小区、地址、联系人…" /></label>
          <div className="filter-tabs">
            <Filter size={16} />
            {[['all', '全部'], ['draft', '记录中'], ['complete', '已完成']].map(([value, label]) => (
              <button className={filter === value ? 'active' : ''} onClick={() => setFilter(value)} key={value}>{label}</button>
            ))}
          </div>
        </section>
      )}

      {rooms.length === 0 ? <EmptyState onCreate={onCreate} /> : (
        <section className="room-grid">
          {filtered.map((room) => <RoomCard key={room.id} room={room} onEdit={onEdit} onDelete={onDelete} />)}
          {filtered.length === 0 && <div className="no-results">没有找到匹配的房源记录。</div>}
        </section>
      )}

      <div className="mobile-backup-bar">
        <button onClick={exportData}><Download size={17} /> 导出</button>
        <button onClick={() => fileRef.current?.click()}><Upload size={17} /> 导入</button>
      </div>
    </main>
  );
}

function Field({ label, hint, children, wide = false }) {
  return (
    <label className={`field ${wide ? 'wide' : ''}`}>
      <span className="field-label">{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}

function SelectInput({ value, onChange, options, placeholder = '暂不填写' }) {
  return (
    <div className="select-wrap">
      <select value={value || ''} onChange={(event) => onChange(event.target.value)}>
        <option value="">{placeholder}</option>
        {options.map((option) => {
          const optionValue = typeof option === 'string' ? option : option.value;
          const label = typeof option === 'string' ? option : option.label;
          return <option key={optionValue} value={optionValue}>{label}</option>;
        })}
      </select>
      <ChevronDown size={17} />
    </div>
  );
}

function Section({ icon: Icon, title, description, children, defaultOpen = true, completion }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <section className={`form-section ${open ? 'open' : ''}`}>
      <button className="section-heading" onClick={() => setOpen((value) => !value)} type="button">
        <div className="section-icon"><Icon size={20} /></div>
        <div><strong>{title}</strong><span>{description}</span></div>
        {completion !== undefined && <span className="section-completion">{completion}</span>}
        <ChevronDown className="section-chevron" size={19} />
      </button>
      {open && <div className="section-content">{children}</div>}
    </section>
  );
}

function CompassField({ room, update }) {
  const [heading, setHeading] = useState(room.directionDegree ?? null);
  const [listening, setListening] = useState(false);
  const [message, setMessage] = useState('站在主要采光窗前，让手机顶部朝向窗外。');

  function handleOrientation(event) {
    let value = event.webkitCompassHeading;
    if (value === undefined && event.alpha !== null) value = 360 - event.alpha;
    if (value === undefined || value === null) return;
    const rounded = Math.round(value);
    setHeading(rounded);
    update('directionDegree', rounded);
    update('direction', cardinalFromDegree(rounded));
  }

  useEffect(() => () => {
    window.removeEventListener('deviceorientationabsolute', handleOrientation, true);
    window.removeEventListener('deviceorientation', handleOrientation, true);
  }, []);

  async function startCompass() {
    try {
      if (typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function') {
        const permission = await DeviceOrientationEvent.requestPermission();
        if (permission !== 'granted') throw new Error('未获得方向传感器权限');
      }
      window.addEventListener('deviceorientationabsolute', handleOrientation, true);
      window.addEventListener('deviceorientation', handleOrientation, true);
      setListening(true);
      setMessage('正在读取方向，稳定几秒后可手动微调。');
    } catch (error) {
      setMessage(error.message || '当前设备无法读取指南针，请手动选择。');
    }
  }

  return (
    <div className="compass-panel">
      <div className="compass-visual">
        <div className="compass-ring" style={{ transform: `rotate(${-Number(heading || 0)}deg)` }}>
          <span className="north">北</span><span className="east">东</span><span className="south">南</span><span className="west">西</span>
          <div className="compass-needle" />
        </div>
        <div className="compass-readout"><strong>{room.direction || '—'}</strong><span>{heading === null ? '未测量' : `${heading}°`}</span></div>
      </div>
      <div className="compass-controls">
        <p>{message}</p>
        <div className="button-row">
          <button type="button" className="secondary-button" onClick={startCompass}><Compass size={17} /> {listening ? '重新校准' : '启动指南针'}</button>
          <SelectInput value={room.direction} onChange={(value) => update('direction', value)} options={['北', '东北', '东', '东南', '南', '西南', '西', '西北']} placeholder="手动选择朝向" />
        </div>
      </div>
    </div>
  );
}

function ScoreItem({ item, value, note, onScore, onNote }) {
  const [noteOpen, setNoteOpen] = useState(Boolean(note));
  return (
    <div className="score-item">
      <div className="score-copy">
        <strong>{item.label}</strong>
        <span>{item.hint}</span>
      </div>
      <div className="score-control">
        <div className="score-buttons" role="radiogroup" aria-label={item.label}>
          {[1, 2, 3, 4, 5].map((score) => (
            <button
              type="button"
              key={score}
              className={Number(value) === score ? 'selected' : ''}
              onClick={() => onScore(Number(value) === score ? '' : score)}
              title={SCORE_TEXT[score]}
            >{score}</button>
          ))}
        </div>
        <span className="score-text">{value ? SCORE_TEXT[value] : '可跳过'}</span>
        <button type="button" className="note-toggle" onClick={() => setNoteOpen((open) => !open)}>{noteOpen ? '收起备注' : '加备注'}</button>
      </div>
      {noteOpen && <textarea value={note || ''} onChange={(event) => onNote(event.target.value)} placeholder={`记录${item.label}的具体观察…`} rows="2" />}
    </div>
  );
}

function PhotoUploader({ photos, onChange }) {
  const inputRef = useRef(null);
  const cameraRef = useRef(null);

  async function loadImage(file) {
    if ('createImageBitmap' in window) return createImageBitmap(file);
    return new Promise((resolve, reject) => {
      const image = new Image();
      const url = URL.createObjectURL(file);
      image.onload = () => {
        URL.revokeObjectURL(url);
        resolve(image);
      };
      image.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error('图片读取失败'));
      };
      image.src = url;
    });
  }

  async function compress(file) {
    const image = await loadImage(file);
    const maxSide = 1600;
    const width = image.width;
    const height = image.height;
    const ratio = Math.min(1, maxSide / Math.max(width, height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(width * ratio);
    canvas.height = Math.round(height * ratio);
    const context = canvas.getContext('2d');
    context.drawImage(image, 0, 0, canvas.width, canvas.height);
    if (typeof image.close === 'function') image.close();
    return new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.78));
  }

  async function addFiles(fileList) {
    const next = [];
    for (const file of Array.from(fileList || []).slice(0, Math.max(0, 12 - photos.length))) {
      try {
        const blob = await compress(file);
        const dataUrl = await new Promise((resolve, reject) => {
          const reader = new FileReader();
          reader.onload = () => resolve(reader.result);
          reader.onerror = reject;
          reader.readAsDataURL(blob);
        });
        next.push({ id: crypto.randomUUID(), name: file.name, dataUrl, createdAt: new Date().toISOString() });
      } catch {
        // Ignore one broken photo and continue with the rest.
      }
    }
    onChange([...photos, ...next]);
  }

  return (
    <div>
      <div className="photo-grid">
        {photos.map((photo, index) => (
          <div className="photo-item" key={photo.id}>
            <img src={photo.dataUrl} alt={`房源照片 ${index + 1}`} />
            {index === 0 && <span>封面</span>}
            <button type="button" onClick={() => onChange(photos.filter((item) => item.id !== photo.id))}><X size={15} /></button>
          </div>
        ))}
        {photos.length < 12 && (
          <button className="photo-add" type="button" onClick={() => inputRef.current?.click()}>
            <ImagePlus size={24} /><span>选择照片</span><small>{photos.length}/12</small>
          </button>
        )}
        {photos.length < 12 && (
          <button className="photo-add camera" type="button" onClick={() => cameraRef.current?.click()}>
            <Camera size={24} /><span>现场拍摄</span><small>调用相机</small>
          </button>
        )}
      </div>
      <input ref={inputRef} hidden type="file" accept="image/*" multiple onChange={(event) => addFiles(event.target.files)} />
      <input ref={cameraRef} hidden type="file" accept="image/*" capture="environment" onChange={(event) => addFiles(event.target.files)} />
      <p className="photo-tip">照片会压缩后保存在当前浏览器中，不会自动上传到服务器。建议拍：门牌/外观、全景、窗外、厨卫、空调铭牌、水表或明显问题。</p>
    </div>
  );
}

function Editor({ initialRoom, onClose, onSaved }) {
  const [room, setRoom] = useState(initialRoom);
  const [saveState, setSaveState] = useState('saved');
  const [mapOpen, setMapOpen] = useState(false);
  const [locationState, setLocationState] = useState('idle');
  const latestRoom = useRef(room);

  useEffect(() => { latestRoom.current = room; }, [room]);

  useEffect(() => {
    setSaveState('saving');
    const timer = window.setTimeout(async () => {
      const saved = { ...latestRoom.current, updatedAt: new Date().toISOString() };
      await saveRoom(saved);
      setSaveState('saved');
      onSaved?.(saved, false);
    }, 650);
    return () => window.clearTimeout(timer);
  }, [room]);

  function update(key, value) {
    setRoom((current) => ({ ...current, [key]: value, updatedAt: new Date().toISOString() }));
  }

  function updateScore(key, value) {
    setRoom((current) => ({ ...current, scores: { ...current.scores, [key]: value }, updatedAt: new Date().toISOString() }));
  }

  function updateScoreNote(key, value) {
    setRoom((current) => ({ ...current, scoreNotes: { ...current.scoreNotes, [key]: value }, updatedAt: new Date().toISOString() }));
  }

  async function locateMe() {
    if (!navigator.geolocation) {
      setLocationState('unsupported');
      return;
    }
    setLocationState('locating');
    navigator.geolocation.getCurrentPosition(async (position) => {
      const { latitude, longitude, accuracy } = position.coords;
      update('latitude', latitude);
      update('longitude', longitude);
      update('locationAccuracy', accuracy);
      try {
        update('address', await reverseGeocode(latitude, longitude));
        setLocationState('done');
      } catch {
        update('address', `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`);
        setLocationState('partial');
      }
    }, () => setLocationState('denied'), { enableHighAccuracy: true, timeout: 12000, maximumAge: 30000 });
  }

  async function finish(status) {
    const saved = { ...room, status, updatedAt: new Date().toISOString() };
    setRoom(saved);
    await saveRoom(saved);
    onSaved(saved, true);
  }

  const average = getAverageScore(room);
  const completion = getCompletion(room);
  const scoreCount = Object.values(room.scores || {}).filter(Boolean).length;

  return (
    <main className="editor-page">
      <header className="editor-topbar">
        <button className="icon-button" onClick={() => onClose(room)} aria-label="返回列表"><ArrowLeft size={21} /></button>
        <div className="editor-title">
          <input value={room.title} onChange={(event) => update('title', event.target.value)} placeholder="给房源起个名字，如：春日公寓 502" />
          <span className={`save-state ${saveState}`}><span /> {saveState === 'saving' ? '正在自动保存' : '已自动保存'}</span>
        </div>
        <button className="ghost-button hide-mobile" onClick={() => finish('draft')}><Save size={17} /> 保存草稿</button>
        <button className="primary-button" onClick={() => finish('complete')}><Check size={18} /> 完成记录</button>
      </header>

      <div className="editor-layout">
        <aside className="editor-summary">
          <div className="summary-sticky">
            <span className="eyebrow">本次看房</span>
            <h2>{room.title || '未命名房源'}</h2>
            <p><MapPin size={15} /> {room.address || '还没有记录地址'}</p>
            <div className="summary-score">
              <div><strong>{average ? average.toFixed(1) : '—'}</strong><span>综合评分 / 5</span></div>
              <div><strong>{scoreCount}</strong><span>已评分项目</span></div>
            </div>
            <div className="completion-card">
              <div><span>记录完整度</span><strong>{completion}%</strong></div>
              <div className="progress-track"><span style={{ width: `${completion}%` }} /></div>
              <p>不必追求填满；只记录真正观察到的内容。</p>
            </div>
            <div className="summary-meta">
              <span>初次记录</span><strong>{formatDate(room.createdAt)}</strong>
              <span>最后编辑</span><strong>{formatDate(room.updatedAt)}</strong>
            </div>
          </div>
        </aside>

        <div className="editor-form">
          <div className="gentle-tip"><ShieldCheck size={19} /><div><strong>现场优先：照片、位置、明显问题</strong><span>所有输入项都可以留空，离开房源后再慢慢补齐。</span></div></div>

          <Section icon={CircleDollarSign} title="基本信息与费用" description="先记硬条件，避免回家后混淆" defaultOpen>
            <div className="field-grid">
              <Field label="月租金（元）"><input inputMode="decimal" value={room.rent} onChange={(e) => update('rent', e.target.value)} placeholder="例如 4500" /></Field>
              <Field label="押金（元）"><input inputMode="decimal" value={room.deposit} onChange={(e) => update('deposit', e.target.value)} placeholder="例如 4500" /></Field>
              <Field label="物业/管理费（元/月）"><input inputMode="decimal" value={room.managementFee} onChange={(e) => update('managementFee', e.target.value)} placeholder="没有可留空" /></Field>
              <Field label="其他固定月费（元）"><input inputMode="decimal" value={room.otherMonthlyCost} onChange={(e) => update('otherMonthlyCost', e.target.value)} placeholder="网络、停车等" /></Field>
              <Field label="付款方式"><input value={room.paymentMethod} onChange={(e) => update('paymentMethod', e.target.value)} placeholder="押一付三 / 月付…" /></Field>
              <Field label="中介费"><input value={room.agencyFee} onChange={(e) => update('agencyFee', e.target.value)} placeholder="金额或比例" /></Field>
              <Field label="户型"><input value={room.layout} onChange={(e) => update('layout', e.target.value)} placeholder="1室1厅 / 单间…" /></Field>
              <Field label="建筑/使用面积（㎡）"><input inputMode="decimal" value={room.area} onChange={(e) => update('area', e.target.value)} placeholder="例如 38" /></Field>
              <Field label="所在楼层"><input inputMode="numeric" value={room.floor} onChange={(e) => update('floor', e.target.value)} placeholder="例如 5" /></Field>
              <Field label="总楼层"><input inputMode="numeric" value={room.totalFloors} onChange={(e) => update('totalFloors', e.target.value)} placeholder="例如 18" /></Field>
              <Field label="电梯"><SelectInput value={room.elevator} onChange={(v) => update('elevator', v)} options={['有', '无', '不确定']} /></Field>
              <Field label="可入住日期"><input type="date" value={room.moveInDate} onChange={(e) => update('moveInDate', e.target.value)} /></Field>
              <Field label="租期"><input value={room.leaseTerm} onChange={(e) => update('leaseTerm', e.target.value)} placeholder="一年 / 可短租…" /></Field>
            </div>
          </Section>

          <Section icon={MapPin} title="位置与联系人" description="支持自动定位、手输地址或地图选点" defaultOpen>
            <div className="address-block">
              <Field label="详细地址" wide><textarea rows="2" value={room.address} onChange={(e) => update('address', e.target.value)} placeholder="小区、楼栋、门牌或地标；也可以先定位后再修改" /></Field>
              <div className="location-actions">
                <button type="button" className="secondary-button" onClick={locateMe}><LocateFixed size={17} /> {locationState === 'locating' ? '定位中…' : '使用当前位置'}</button>
                <button type="button" className="secondary-button" onClick={() => setMapOpen(true)}><MapPin size={17} /> 打开地图选择</button>
                {room.latitude && <span className="coordinate-note">{Number(room.latitude).toFixed(5)}, {Number(room.longitude).toFixed(5)}{room.locationAccuracy ? ` · 约 ±${Math.round(room.locationAccuracy)}m` : ''}</span>}
              </div>
              {['denied', 'unsupported', 'partial'].includes(locationState) && <p className="inline-warning">{locationState === 'denied' ? '未获得定位权限，可以手动输入或从地图选择。' : locationState === 'unsupported' ? '当前浏览器不支持定位。' : '已取得坐标，但地址识别失败，可手动补充。'}</p>}
            </div>
            <div className="field-grid contact-grid">
              <Field label="联系人"><input value={room.contactName} onChange={(e) => update('contactName', e.target.value)} placeholder="房东 / 中介姓名" /></Field>
              <Field label="联系电话"><input inputMode="tel" value={room.contactPhone} onChange={(e) => update('contactPhone', e.target.value)} placeholder="手机号或其他方式" /></Field>
              <Field label="联系人类型"><SelectInput value={room.contactType} onChange={(v) => update('contactType', v)} options={['房东', '中介', '转租人', '公寓管家', '其他']} /></Field>
              <Field label="联系备注"><input value={room.contactNote} onChange={(e) => update('contactNote', e.target.value)} placeholder="微信、可联系时段、承诺事项…" /></Field>
            </div>
          </Section>

          <Section icon={Camera} title="现场照片" description="最多 12 张，首张作为列表封面" defaultOpen>
            <PhotoUploader photos={room.photos || []} onChange={(photos) => update('photos', photos)} />
          </Section>

          <Section icon={Compass} title="朝向与主要设施" description="指南针可自动判断，也可全部手动选择" defaultOpen>
            <CompassField room={room} update={update} />
            <div className="field-grid feature-grid">
              <Field label="独立阳台"><SelectInput value={room.balcony} onChange={(v) => update('balcony', v)} options={['有独立阳台', '无阳台', '共用阳台', '仅飘窗', '不确定']} /></Field>
              <Field label="空调能耗"><SelectInput value={room.acEnergy} onChange={(v) => update('acEnergy', v)} options={['无空调', '一级能效', '二级能效', '三级能效', '四/五级或老旧', '有但未确认']} /></Field>
              <Field label="洗衣机"><SelectInput value={room.washingMachine} onChange={(v) => update('washingMachine', v)} options={['室内独立', '阳台独立', '公共洗衣机', '仅预留位置', '没有', '不确定']} /></Field>
              <Field label="取暖"><SelectInput value={room.heating} onChange={(v) => update('heating', v)} options={['集中供暖', '地暖', '空调取暖', '燃气/电暖', '无', '不确定']} /></Field>
              <Field label="做饭条件"><SelectInput value={room.cooking} onChange={(v) => update('cooking', v)} options={['燃气', '电磁炉', '不可明火', '无厨房', '不确定']} /></Field>
              <Field label="宠物限制"><SelectInput value={room.pets} onChange={(v) => update('pets', v)} options={['允许', '不允许', '协商', '不确定']} /></Field>
              <Field label="停车/自行车"><input value={room.parking} onChange={(e) => update('parking', e.target.value)} placeholder="车位、费用、充电条件…" /></Field>
              <Field label="水电燃气计费"><input value={room.utilityBilling} onChange={(e) => update('utilityBilling', e.target.value)} placeholder="民用 / 商用 / 包含在租金…" /></Field>
            </div>
          </Section>

          <Section icon={Sparkles} title="逐项评分" description="只给亲自确认过的项目评分；1 很差，5 很好" completion={`${scoreCount}/${SCORE_FIELDS.length}`} defaultOpen>
            <div className="score-legend"><span>1 很差</span><span>3 一般</span><span>5 很好</span></div>
            <div className="score-list">
              {SCORE_FIELDS.map((item) => (
                <ScoreItem
                  key={item.key}
                  item={item}
                  value={room.scores?.[item.key]}
                  note={room.scoreNotes?.[item.key]}
                  onScore={(value) => updateScore(item.key, value)}
                  onNote={(value) => updateScoreNote(item.key, value)}
                />
              ))}
            </div>
          </Section>

          <Section icon={FilePlus2} title="结论与待确认" description="把影响决策的信息单独拎出来" defaultOpen>
            <div className="field-grid conclusion-grid">
              <Field label="明显优点" wide><textarea rows="4" value={room.pros} onChange={(e) => update('pros', e.target.value)} placeholder="例如：离地铁近、采光好、房东直租…" /></Field>
              <Field label="明显缺点 / 风险" wide><textarea rows="4" value={room.cons} onChange={(e) => update('cons', e.target.value)} placeholder="例如：临街噪音、墙角返潮、商水商电…" /></Field>
              <Field label="还要问清楚的问题" wide><textarea rows="4" value={room.questions} onChange={(e) => update('questions', e.target.value)} placeholder="退租规则、维修责任、家具能否搬走、能否办居住登记…" /></Field>
              <Field label="其他记录" wide><textarea rows="5" value={room.overallNote} onChange={(e) => update('overallNote', e.target.value)} placeholder="任何无法归类但值得记住的细节" /></Field>
            </div>
          </Section>

          <div className="editor-bottom-actions">
            <button className="secondary-button" onClick={() => finish('draft')}><Save size={18} /> 保存并稍后继续</button>
            <button className="primary-button large" onClick={() => finish('complete')}><Check size={19} /> 完成这次记录</button>
          </div>
        </div>
      </div>

      {mapOpen && (
        <MapPicker
          initialPosition={room.latitude && room.longitude ? [room.latitude, room.longitude] : null}
          onClose={() => setMapOpen(false)}
          onConfirm={({ latitude, longitude, address }) => {
            update('latitude', latitude);
            update('longitude', longitude);
            if (address) update('address', address);
            setMapOpen(false);
          }}
        />
      )}
    </main>
  );
}

function ConfirmDialog({ title, body, confirmText = '删除', onCancel, onConfirm }) {
  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="confirm-modal">
        <div className="danger-icon"><Trash2 size={22} /></div>
        <h3>{title}</h3>
        <p>{body}</p>
        <div className="confirm-actions">
          <button className="secondary-button" onClick={onCancel}>取消</button>
          <button className="danger-button" onClick={onConfirm}>{confirmText}</button>
        </div>
      </div>
    </div>
  );
}

export default function App() {
  const [rooms, setRooms] = useState([]);
  const [editing, setEditing] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState('');

  useEffect(() => {
    getAllRooms().then((items) => {
      setRooms(items);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(''), 2600);
    return () => clearTimeout(timer);
  }, [toast]);

  function createRoom() {
    setEditing(emptyRoom());
  }

  function mergeRoom(saved, close) {
    setRooms((current) => {
      const exists = current.some((item) => item.id === saved.id);
      return exists ? current.map((item) => item.id === saved.id ? saved : item) : [saved, ...current];
    });
    if (close) {
      setEditing(null);
      setToast(saved.status === 'complete' ? '看房记录已完成' : '草稿已保存');
    }
  }

  async function confirmDelete() {
    await deleteRoom(deleteTarget.id);
    setRooms((current) => current.filter((room) => room.id !== deleteTarget.id));
    setDeleteTarget(null);
    setToast('记录已删除');
  }

  async function importData(file) {
    if (!file) return;
    try {
      const data = JSON.parse(await file.text());
      if (!Array.isArray(data.rooms)) throw new Error('格式不正确');
      const normalized = data.rooms.map((room) => ({ ...emptyRoom(), ...room, id: room.id || crypto.randomUUID() }));
      await replaceAllRooms(normalized);
      setRooms(normalized);
      setToast(`已导入 ${normalized.length} 条记录`);
    } catch {
      setToast('导入失败：请选择本应用导出的 JSON 文件');
    }
  }

  if (loading) return <div className="loading-screen"><div className="brand-mark"><Building2 size={24} /></div><span>正在读取看房记录…</span></div>;

  return (
    <>
      {editing ? (
        <Editor initialRoom={editing} onClose={(room) => { mergeRoom(room, false); setEditing(null); }} onSaved={mergeRoom} />
      ) : (
        <Dashboard rooms={rooms} onCreate={createRoom} onEdit={setEditing} onDelete={setDeleteTarget} onImport={importData} />
      )}
      {deleteTarget && <ConfirmDialog title="删除这条看房记录？" body={`“${deleteTarget.title || deleteTarget.address || '未命名房源'}”的照片、评分和备注都会一并删除，且无法撤销。`} onCancel={() => setDeleteTarget(null)} onConfirm={confirmDelete} />}
      {toast && <div className="toast"><Check size={17} /> {toast}</div>}
    </>
  );
}
