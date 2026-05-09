import { useState, useEffect } from 'react'
import { Card, Form, Input, Button, Table, Tag, message, Spin, Descriptions, Divider, Space } from 'antd'
import { RobotOutlined, SendOutlined, ReloadOutlined } from '@ant-design/icons'
import { submitArticle, getArticleAnalyses } from '../services/api'

const { TextArea } = Input

export default function ArticleAnalysis() {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [list, setList] = useState([])

  const fetchList = () => {
    setLoading(true)
    getArticleAnalyses({ account_id: 1, page: 0, size: 20 })
      .then(res => setList(res.data?.items || []))
      .catch(err => message.error(err))
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchList() }, [])

  const handleSubmit = async (values) => {
    if (!values.title || !values.content) {
      message.warning('请填写标题和正文')
      return
    }
    setSubmitting(true)
    setResult(null)
    try {
      const res = await submitArticle({
        account_id: 1,
        title: values.title,
        content: values.content,
        author: values.author || '匿名'
      })
      setResult(res.data)
      message.success('AI分析完成！')
      fetchList()
    } catch (err) {
      message.error(err)
    } finally {
      setSubmitting(false)
    }
  }

  const columns = [
    { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true, width: 250 },
    { title: '质量分', dataIndex: 'quality_score', key: 'quality_score', render: v => v != null ? <Tag color={v >= 8 ? 'green' : v >= 6 ? 'orange' : 'red'}>{v}</Tag> : '-' },
    { title: '标题吸引力', dataIndex: 'title_attraction_score', key: 'title_attraction_score', render: v => v != null ? `${v}/100` : '-' },
    { title: '标题党', dataIndex: 'is_title_clickbaity', key: 'is_title_clickbaity', render: v => v ? <Tag color="red">是</Tag> : <Tag>否</Tag> },
    {
      title: '标签', dataIndex: 'tags', key: 'tags',
      render: tags => tags && tags.length > 0 ? tags.map(t => <Tag key={t.tag || t}>{t.tag || t}</Tag>) : '-'
    },
    {
      title: '摘要', dataIndex: 'ai_summary', key: 'ai_summary', ellipsis: true
    },
  ]

  return (
    <div>
      <Card title={<><RobotOutlined /> AI文章分析</>} style={{ marginBottom: 24 }}>
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="文章标题" name="title" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="输入公众号文章标题" size="large" />
          </Form.Item>
          <Form.Item label="文章作者" name="author">
            <Input placeholder="作者名（选填）" />
          </Form.Item>
          <Form.Item label="文章正文" name="content" rules={[{ required: true, message: '请输入正文' }]}>
            <TextArea rows={8} placeholder="粘贴公众号文章正文内容（AI会分析前1500字）" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<SendOutlined />} loading={submitting} size="large">
              提交AI分析
            </Button>
            <Button icon={<ReloadOutlined />} onClick={() => { form.resetFields(); setResult(null) }} style={{ marginLeft: 12 }}>
              重置
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {submitting && <Spin tip="AI正在分析中..." style={{ display: 'block', margin: '40px auto' }} />}

      {result && (
        <Card title="AI分析结果" style={{ marginBottom: 24 }}>
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="文章标题">{result.title}</Descriptions.Item>
            <Descriptions.Item label="综合质量分">
              <Tag color={result.quality_score >= 8 ? 'green' : result.quality_score >= 6 ? 'orange' : 'red'}>
                {result.quality_score}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="标题吸引力">{result.title_attraction_score}/100</Descriptions.Item>
            <Descriptions.Item label="疑似标题党">{result.is_title_clickbaity ? '是' : '否'}</Descriptions.Item>
            <Descriptions.Item label="标签" span={2}>
              {result.tags && result.tags.length > 0
                ? result.tags.map(t => <Tag key={t.tag || t} color="blue">{t.tag || t} ({(t.weight * 100).toFixed(0)}%)</Tag>)
                : '无'}
            </Descriptions.Item>
            <Descriptions.Item label="AI摘要" span={2}>{result.ai_summary || '无'}</Descriptions.Item>
            <Descriptions.Item label="各维度评分" span={2}>
              {result.dimension_scores && Object.entries(result.dimension_scores).map(([k, v]) =>
                k !== 'overall' ? <Tag key={k}>{k}: {v}分</Tag> : null
              )}
            </Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      <Card title="历史分析记录" extra={<Button size="small" icon={<ReloadOutlined />} onClick={fetchList}>刷新</Button>}>
        <Table dataSource={list} columns={columns} rowKey="id" loading={loading} pagination={{ pageSize: 10 }} />
      </Card>
    </div>
  )
}
