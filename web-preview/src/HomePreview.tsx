import { 
  Image as ImageIcon, 
  Clock, 
  CheckCircle2, 
  Plus, 
  User,
  Settings,
  History,
  Globe,
  Shield,
  Palette,
  Camera,
  X
} from 'lucide-react';
import { useState } from 'react';

interface HomePreviewProps {
  onNavigateToEditor: () => void;
}

export default function HomePreview({ onNavigateToEditor }: HomePreviewProps) {
  const [activeTab, setActiveTab] = useState<'home' | 'profile'>('home');
  const [showImageSourceDialog, setShowImageSourceDialog] = useState(false);

  const drafts = [
    { id: 1, name: '风景照片.jpg', time: '2分钟前', thumbnail: '🏔️' },
    { id: 2, name: '人像修图.jpg', time: '1小时前', thumbnail: '👤' },
  ];

  const completed = [
    { id: 3, name: '美食照片.jpg', time: '昨天', thumbnail: '🍜' },
    { id: 4, name: '旅行纪念.jpg', time: '3天前', thumbnail: '✈️' },
  ];

  return (
    <div style={{
      width: '100%',
      height: '100vh',
      background: '#f5f5f5',
      display: 'flex',
      flexDirection: 'column',
      maxWidth: '430px',
      margin: '0 auto',
      boxShadow: '0 0 20px rgba(0,0,0,0.1)'
    }}>
      {/* Header */}
      <div style={{
        background: 'linear-gradient(135deg, #00C9A7 0%, #00A896 100%)',
        padding: '16px 20px',
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
      }}>
        <h1 style={{ fontSize: '22px', fontWeight: 'bold' }}>
          {activeTab === 'home' ? 'PhotoPS' : '我的'}
        </h1>
        {activeTab === 'home' && (
          <button
            onClick={() => setShowImageSourceDialog(true)}
            style={{
              background: 'white',
              border: 'none',
              borderRadius: '20px',
              padding: '8px 16px',
              color: '#00C9A7',
              fontSize: '14px',
              fontWeight: '500',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              cursor: 'pointer'
            }}
          >
            <Plus size={16} />
            新建
          </button>
        )}
      </div>

      {/* Content */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        {activeTab === 'home' ? (
          <div style={{ padding: '16px' }}>
            {/* Drafts Section */}
            <div style={{ marginBottom: '24px' }}>
              <h2 style={{ 
                fontSize: '18px', 
                fontWeight: 'bold', 
                marginBottom: '12px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <Clock size={20} />
                正在编辑
              </h2>
              {drafts.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {drafts.map(draft => (
                    <div
                      key={draft.id}
                      onClick={onNavigateToEditor}
                      style={{
                        background: 'white',
                        borderRadius: '12px',
                        padding: '12px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px',
                        cursor: 'pointer',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
                      }}
                    >
                      <div style={{
                        width: '60px',
                        height: '60px',
                        background: '#f0f0f0',
                        borderRadius: '8px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '28px'
                      }}>
                        {draft.thumbnail}
                      </div>
                      <div style={{ flex: 1 }}>
                        <p style={{ fontWeight: '600', marginBottom: '4px' }}>{draft.name}</p>
                        <p style={{ fontSize: '12px', color: '#999' }}>{draft.time}</p>
                      </div>
                      <div style={{
                        padding: '6px 12px',
                        background: '#667eea',
                        color: 'white',
                        borderRadius: '16px',
                        fontSize: '12px'
                      }}>
                        继续
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div style={{
                  background: 'white',
                  borderRadius: '12px',
                  padding: '32px',
                  textAlign: 'center',
                  color: '#999'
                }}>
                  <Clock size={48} style={{ marginBottom: '12px', opacity: 0.5 }} />
                  <p>暂无正在编辑的照片</p>
                </div>
              )}
            </div>

            {/* Completed Section */}
            <div>
              <h2 style={{ 
                fontSize: '18px', 
                fontWeight: 'bold', 
                marginBottom: '12px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}>
                <CheckCircle2 size={20} />
                已完成
              </h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {completed.map(item => (
                  <div
                    key={item.id}
                    style={{
                      background: 'white',
                      borderRadius: '12px',
                      padding: '12px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
                    }}
                  >
                    <div style={{
                      width: '60px',
                      height: '60px',
                      background: '#f0f0f0',
                      borderRadius: '8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '28px'
                    }}>
                      {item.thumbnail}
                    </div>
                    <div style={{ flex: 1 }}>
                      <p style={{ fontWeight: '600', marginBottom: '4px' }}>{item.name}</p>
                      <p style={{ fontSize: '12px', color: '#999' }}>{item.time}</p>
                    </div>
                    <CheckCircle2 size={20} color="#4CAF50" />
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : (
          <div style={{ padding: '16px', overflowY: 'auto', height: '100%' }}>
            {/* Profile Header */}
            <div style={{
              background: 'white',
              borderRadius: '16px',
              padding: '24px',
              textAlign: 'center',
              marginBottom: '16px'
            }}>
              <div style={{
                width: '80px',
                height: '80px',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                borderRadius: '50%',
                margin: '0 auto 16px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <User size={40} color="white" />
              </div>
              <h3 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '4px' }}>
                游客用户
              </h3>
              <p style={{ color: '#999', fontSize: '14px' }}>点击登录账号</p>
            </div>

            {/* Remaining Time Card */}
            <div style={{
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              borderRadius: '16px',
              padding: '20px',
              marginBottom: '16px',
              color: 'white'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <Clock size={32} />
                <div>
                  <p style={{ fontSize: '14px', opacity: 0.9 }}>剩余使用时间</p>
                  <p style={{ fontSize: '24px', fontWeight: 'bold' }}>5 天 23 小时</p>
                </div>
              </div>
            </div>

            {/* Section Title */}
            <p style={{ 
              fontSize: '13px', 
              fontWeight: 'bold', 
              color: '#667eea',
              marginBottom: '8px',
              marginLeft: '4px',
              textTransform: 'uppercase',
              letterSpacing: '0.5px'
            }}>
              应用设置
            </p>

            {/* Settings List */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '16px' }}>
              <SettingItem icon={<Palette size={20} />} title="外观风格" subtitle="深色模式" />
              <SettingItem icon={<Globe size={20} />} title="语言设置" subtitle="简体中文" />
            </div>

            {/* Section Title */}
            <p style={{ 
              fontSize: '13px', 
              fontWeight: 'bold', 
              color: '#667eea',
              marginBottom: '8px',
              marginLeft: '4px',
              textTransform: 'uppercase',
              letterSpacing: '0.5px'
            }}>
              权限管理
            </p>

            {/* Permissions List */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '16px' }}>
              <SettingItem icon={<ImageIcon size={20} />} title="相册访问权限" subtitle="已授权" />
              <SettingItem icon={<Shield size={20} />} title="相机权限" subtitle="未授权" />
              <SettingItem icon={<Settings size={20} />} title="存储权限" subtitle="已授权" />
            </div>

            {/* Section Title */}
            <p style={{ 
              fontSize: '13px', 
              fontWeight: 'bold', 
              color: '#667eea',
              marginBottom: '8px',
              marginLeft: '4px',
              textTransform: 'uppercase',
              letterSpacing: '0.5px'
            }}>
              编辑记录
            </p>

            {/* History List */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '16px' }}>
              <SettingItem icon={<History size={20} />} title="编辑历史" subtitle="查看所有编辑记录" />
              <SettingItem icon={<CheckCircle2 size={20} />} title="设备信息" subtitle="Android 14 • v1.0.0" />
            </div>

            {/* Version Info */}
            <div style={{ textAlign: 'center', padding: '24px', color: '#999' }}>
              <p style={{ fontSize: '14px', marginBottom: '4px' }}>PhotoPS 专业版</p>
              <p style={{ fontSize: '12px' }}>版本 1.0.0 (Build 20240115)</p>
            </div>

            {/* Extra space for scroll demonstration */}
            <div style={{ height: '50px' }} />
          </div>
        )}
      </div>

      {/* Bottom Navigation */}
      <div style={{
        background: 'white',
        borderTop: '1px solid #eee',
        display: 'flex',
        justifyContent: 'space-around',
        padding: '8px 0'
      }}>
        <NavButton
          icon={<ImageIcon size={24} />}
          label="首页"
          isActive={activeTab === 'home'}
          onClick={() => setActiveTab('home')}
        />
        <NavButton
          icon={<User size={24} />}
          label="我的"
          isActive={activeTab === 'profile'}
          onClick={() => setActiveTab('profile')}
        />
      </div>

      {/* Image Source Bottom Sheet */}
      {showImageSourceDialog && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'center',
          zIndex: 1000
        }} onClick={() => setShowImageSourceDialog(false)}>
          <div style={{
            background: 'white',
            width: '100%',
            maxWidth: '430px',
            borderRadius: '20px 20px 0 0',
            padding: '20px'
          }} onClick={e => e.stopPropagation()}>
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '20px'
            }}>
              <h3 style={{ fontSize: '18px', fontWeight: 'bold' }}>选择图片来源</h3>
              <button 
                onClick={() => setShowImageSourceDialog(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer' }}
              >
                <X size={24} />
              </button>
            </div>
            
            <div 
              onClick={() => { onNavigateToEditor(); setShowImageSourceDialog(false); }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                padding: '16px 0',
                cursor: 'pointer',
                borderBottom: '1px solid #eee'
              }}
            >
              <Camera size={28} color="#00C9A7" />
              <span style={{ fontSize: '16px' }}>拍照</span>
            </div>
            
            <div 
              onClick={() => { onNavigateToEditor(); setShowImageSourceDialog(false); }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                padding: '16px 0',
                cursor: 'pointer'
              }}
            >
              <ImageIcon size={28} color="#00C9A7" />
              <span style={{ fontSize: '16px' }}>从相册选择</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function NavButton({ 
  icon, 
  label, 
  isActive, 
  onClick 
}: { 
  icon: React.ReactNode; 
  label: string; 
  isActive: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '4px',
        padding: '8px 24px',
        background: 'transparent',
        border: 'none',
        cursor: 'pointer',
        color: isActive ? '#667eea' : '#999'
      }}
    >
      {icon}
      <span style={{ fontSize: '12px' }}>{label}</span>
    </button>
  );
}

function SettingItem({ 
  icon, 
  title, 
  subtitle 
}: { 
  icon: React.ReactNode; 
  title: string; 
  subtitle: string;
}) {
  return (
    <div style={{
      background: 'white',
      borderRadius: '12px',
      padding: '16px',
      display: 'flex',
      alignItems: 'center',
      gap: '16px',
      cursor: 'pointer'
    }}>
      <div style={{ color: '#667eea' }}>{icon}</div>
      <div style={{ flex: 1 }}>
        <p style={{ fontWeight: '600', marginBottom: '2px' }}>{title}</p>
        <p style={{ fontSize: '12px', color: '#999' }}>{subtitle}</p>
      </div>
    </div>
  );
}
