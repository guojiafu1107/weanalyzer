import { Menu } from 'antd'
import { DashboardOutlined, FileTextOutlined, RobotOutlined } from '@ant-design/icons'
import { useLocation, useNavigate } from 'react-router-dom'

const items = [
  { key: '/', icon: <DashboardOutlined />, label: '数据概览' },
  { key: '/articles', icon: <FileTextOutlined />, label: '文章分析' },
  { key: '/diagnosis', icon: <RobotOutlined />, label: 'AI运营诊断' },
]

export default function NavMenu() {
  const location = useLocation()
  const navigate = useNavigate()
  return (
    <Menu
      mode="inline"
      selectedKeys={[location.pathname]}
      items={items}
      onClick={({ key }) => navigate(key)}
      style={{ borderRight: 0 }}
    />
  )
}
