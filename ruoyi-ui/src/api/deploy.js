import request from '@/utils/request'

export function listTask(query) {
  let createRange = {}
  if (query.dateRange && query.dateRange.length === 2) {
    createRange.startTime = query.dateRange[0]
    createRange.endTime = query.dateRange[1]
  }
  return request({
    url: '/deploy/list',
    method: 'get',
    params: {...query, ...createRange}
  })
}

export function saveDll(row) {
  return request({
    url: '/deploy/saveDll',
    method: 'get',
    params: {id: row.id, outDlls: row.dlls.join(",")}
  })
}

export function addTask(form) {
  return request({
    url: '/deploy/addTask',
    method: 'post',
    data: form
  })
}
