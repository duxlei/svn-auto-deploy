import request from '@/utils/request'
import qs from 'qs'

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

export function doDeploy(taskIds, env) {
  let queryStr = qs.stringify({"taskIds": taskIds, "env": env}, {arrayFormat: 'repeat'});
  return request({
    url: '/deploy/deploy?' + queryStr,
    method: 'post'
  })
}

export function deployDetail(data) {
  return request({
    url: '/deploy/detail',
    method: 'get',
    params: {"taskId": data.id}
  })
}
