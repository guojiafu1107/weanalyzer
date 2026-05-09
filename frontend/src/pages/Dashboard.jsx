import { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, Table, Tag, message, Spin, Empty } from 'antd'
import { EyeOutlined, ShareAltOutlined, LikeOutlined, FileTextOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import { getDashboard } from '../services/api'

export default function Dashboard() {
  const [loading, setLoading] = useState(true)
  const [data, setData] = useState({ stats: {}, tag_distribution: [], recent_articles: [] })

  useEffect(() => {
    setLoading(true)
    getDashboard(1)
      .then(res => setData(res.data))
      .catch(err => message.error(err))
      .finally(() => setLoading(false))
  }, [])

  const { stats, tag_distribution, recent_articles } = data

  const trendOption = recent_articles.length > 0 ? {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: recent_articles.map(a => a.title.substring(0, 10) + '...'), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value' },
    series: [
      { name: '阅读量', type: 'line', smooth: true, data: recent_articles.map(a => a.read_count), areaStyle: { color: 'rgba(22,119,255,0.1)' }, lineStyle: { color: '#1677ff' }, itemStyle: { color: '#1677ff' } }
    ]
  } : null

  const tagOption = tag_distribution.length > 0 ? {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: tag_distribution
    }]
  } : null

  const columns = [
    { title: '文章标题', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '阅读量', dataIndex: 'read_count', key: 'read_count', sorter: (a, b) => a.read_count - b.read_count },
    { title: '点赞', dataIndex: 'like_count', key: 'like_count' },
    { title: '分享', dataIndex: 'share_count', key: 'share_count' },
    {
      title: '质量分', dataIndex: 'quality_score', key: 'quality_score',
      render: v => v != null ? <Tag color={v >= 8 ? 'green' : v >= 6 ? 'orange' : 'red'}>{v}</Tag> : <Tag>待分析</Tag>
    },
    {
      title: '标签', dataIndex: 'tags', key: 'tags',
      render: tags => tags && tags.length > 0
        ? tags.map(t => <Tag key={t.tag || t}>{t.tag || t}</Tag>)
        : <Tag>无</Tag>
    },
  ]

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card><Statistic title="总阅读量" value={stats.total_read || 0} prefix={<EyeOutlined />} valueStyle={{ color: '#1677ff' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="总点赞" value={stats.total_like || 0} prefix={<LikeOutlined />} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="总分享" value={stats.total_share || 0} prefix={<ShareAltOutlined />} valueStyle={{ color: '#faad14' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="文章数" value={stats.article_count || 0} prefix={<FileTextOutlined />} valueStyle={{ color: '#722ed1' }} /></Card>
        </Col>
      </Row>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={16}>
          <Card title="文章阅读趋势">
            {trendOption ? <ReactECharts option={trendOption} style={{ height: 300 }} /> : <Empty description="暂无数据" />}
          </Card>
        </Col>
        <Col span={8}>
          <Card title="内容标签分布">
            {tagOption ? <ReactECharts option={tagOption} style={{ height: 300 }} /> : <Empty description="暂无数据" />}
          </Card>
        </Col>
      </Row>
      <Card title="文章分析列表">
        <Table dataSource={recent_articles} columns={columns} rowKey="id" pagination={{ pageSize: 10 }} />
      </Card>
    </div>
  )
}
