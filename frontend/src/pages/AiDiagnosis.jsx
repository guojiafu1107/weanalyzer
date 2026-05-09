import { useState } from 'react'
import { Card, Button, Input, List, Tag, message, Spin, Typography, Divider, Space } from 'antd'
import { RobotOutlined, BulbOutlined, FireOutlined } from '@ant-design/icons'
import { getAccountDiagnosis, recommendTopics } from '../services/api'

const { Title, Paragraph } = Typography

export default function AiDiagnosis() {
  const [loading, setLoading] = useState(false)
  const [diagnosis, setDiagnosis] = useState(null)
  const [topics, setTopics] = useState([])
  const [keywords, setKeywords] = useState('')
  const [diagnosisDays, setDiagnosisDays] = useState(7)

  const fetchDiagnosis = async () => {
    if (!keywords.trim()) {
      message.warning('请先输入分析关键词或问题背景')
    }
    setLoading(true)
    try {
      const res = await getAccountDiagnosis(1, diagnosisDays)
      setDiagnosis(res.data)
    } catch (err) {
      message.error(err)
    } finally {
      setLoading(false)
    }
  }

  const fetchTopics = async () => {
    const kw = keywords.split(/[,，、]/).map(k => k.trim()).filter(k => k)
    if (kw.length === 0) {
      message.warning('请输入热点关键词，用逗号分隔')
      return
    }
    setLoading(true)
    try {
      const res = await recommendTopics(1, kw)
      setTopics(res.data || [])
    } catch (err) {
      message.error(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <Card title={<><RobotOutlined /> AI运营助手</>} style={{ marginBottom: 24 }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input.TextArea
            placeholder="输入分析关键词或问题背景，如：最近阅读量下滑、粉丝增长缓慢、内容方向调整等"
            rows={3}
            value={keywords}
            onChange={e => setKeywords(e.target.value)}
          />
          <Space>
            <Button type="primary" icon={<RobotOutlined />} onClick={fetchDiagnosis} loading={loading}>
              AI运营诊断
            </Button>
            <Button icon={<FireOutlined />} onClick={fetchTopics} loading={loading}>
              热点选题推荐
            </Button>
          </Space>
        </Space>
      </Card>

      {diagnosis && (
        <Card title={<><RobotOutlined /> AI运营诊断报告</>} style={{ marginBottom: 24 }}>
          <Title level={5}>总体概览</Title>
          <Paragraph style={{ whiteSpace: 'pre-wrap' }}>{diagnosis.summary}</Paragraph>
          <Divider />
          <Title level={5}>趋势判断</Title>
          <Tag color={diagnosis.trend === '上升' ? 'green' : diagnosis.trend === '下降' ? 'red' : 'blue'}>{diagnosis.trend}</Tag>
          <Divider />
          <Title level={5}>核心建议</Title>
          <List
            dataSource={diagnosis.suggestions || []}
            renderItem={item => (
              <List.Item>
                <BulbOutlined style={{ color: '#faad14', marginRight: 8 }} />
                {item}
              </List.Item>
            )}
          />
          {diagnosis.riskPoints?.length > 0 && (
            <>
              <Divider />
              <Title level={5}>风险提示</Title>
              {diagnosis.riskPoints.map((r, i) => <Tag key={i} color="red" style={{ marginBottom: 4 }}>{r}</Tag>)}
            </>
          )}
        </Card>
      )}

      {topics.length > 0 && (
        <Card title={<><FireOutlined /> AI选题推荐</>}>
          <List
            dataSource={topics}
            renderItem={item => (
              <List.Item>
                <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{item}</Paragraph>
              </List.Item>
            )}
          />
        </Card>
      )}
    </div>
  )
}
