import { Routes, Route } from 'react-router-dom'
import { Layout } from 'antd'
import Dashboard from './pages/Dashboard'
import ArticleAnalysis from './pages/ArticleAnalysis'
import AiDiagnosis from './pages/AiDiagnosis'
import NavMenu from './components/NavMenu'

const { Header, Sider, Content } = Layout

function App() {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="light" width={200}>
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, fontWeight: 'bold', borderBottom: '1px solid #f0f0f0' }}>
          WeAnalyzer
        </div>
        <NavMenu />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', fontSize: 16, fontWeight: 500, borderBottom: '1px solid #f0f0f0' }}>
          微信公众号文章数据分析工具
        </Header>
        <Content style={{ margin: 24, padding: 24, background: '#fff', borderRadius: 8 }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/articles" element={<ArticleAnalysis />} />
            <Route path="/diagnosis" element={<AiDiagnosis />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}

export default App
