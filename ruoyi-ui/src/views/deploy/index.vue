<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="发布环境" prop="status">
        <el-input
          :value="$route.meta.title"
          :disabled="true"
          style="width: 120px"
        />
      </el-form-item>
      <el-form-item label="任务名称" prop="roleName">
        <el-input
          v-model="queryParams.demandName"
          placeholder="请输入任务名称"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="JIRA号" prop="roleKey">
        <el-input
          v-model="queryParams.jiraNo"
          placeholder="请输入JIRA号"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="任务状态"
          clearable
          style="width: 120px"
        >
          <el-option
            v-for="dict in dict.type.deploy_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="需求发布周">
        <el-date-picker
          v-model="queryParams.iterateWeek"
          style="width: 130px"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="选择日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="queryParams.dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >新增单发布任务</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-menu"
          size="mini"
          @click="openImport = true"
        >新增批量发布任务（导入Excel）</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-cpu"
          size="mini"
          @click="batchDeploy"
          v-loading.fullscreen.lock="fullscreenLoading"
        >一键发布</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" ref="taskTable" :data="taskList" @selection-change="taskSelect">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="JIRA编号" prop="jiraNo" width="120" />
      <el-table-column label="需求名称" prop="demandName" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="类型" prop="demandType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.deploy_task_type" :value="scope.row.demandType"/>
        </template>
      </el-table-column>
      <el-table-column label="关联业务需求" prop="relateDemand" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="责任人" prop="principal" width="100" />
      <el-table-column label="需求迭代周" prop="iterateWeek" width="100">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.iterateWeek, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" :show-overflow-tooltip="true" width="250" />
      <el-table-column label="要编译的DLL" prop="outDlls" class-name="fixed-width">
        <template slot-scope="scope">
          <el-select
            multiple
            v-model="scope.row.dlls"
            :disabled="!scope.row.editDlls">
            <el-option
              v-for="dict in dict.type.out_dll"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"/>
          </el-select>
          &nbsp;&nbsp;
          <el-button type="success" icon="el-icon-check" size="mini" v-if="scope.row.editDlls" circle @click="saveDll(scope.row)"></el-button>
          <el-button type="primary" icon="el-icon-edit" size="mini" v-if="!scope.row.editDlls" circle @click="scope.row.editDlls = !scope.row.editDlls"></el-button>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.deploy_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="150">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding" width="180">
        <template slot-scope="scope" v-if="scope.row.roleId !== 1">
          <el-button
            size="mini"
            type="warning"
            @click="doDeploy(scope.row)"
            v-hasPermi="['system:role:edit']"
            v-loading.fullscreen.lock="fullscreenLoading"
          >发布</el-button>
          <el-button
            size="mini"
            @click="deployDetail(scope.row)"
            v-hasPermi="['system:role:edit']"
          >查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新建发布任务对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="JIRA编号" prop="jiraNo">
          <el-input v-model="form.jiraNo" placeholder="请输入JIRA编号" />
        </el-form-item>
        <el-form-item label="需求名称" prop="demandName">
          <el-input v-model="form.demandName" placeholder="请输入需求名称" />
        </el-form-item>
        <el-form-item label="类型" prop="demandType">
            <el-select v-model="form.demandType">
              <el-option
                v-for="dict in dict.type.deploy_task_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"/>
            </el-select>
        </el-form-item>
        <el-form-item label="要编译的DLL" prop="dlls">
            <el-select multiple v-model="form.dlls">
              <el-option
                v-for="dict in dict.type.out_dll"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"/>
            </el-select>
        </el-form-item>
        <el-form-item label="关联业务需求" prop="relateDemand">
          <el-input v-model="form.relateDemand" />
        </el-form-item>
        <el-form-item label="责任人" prop="principal">
          <el-input v-model="form.principal" />
        </el-form-item>
        <el-form-item label="需求迭代周" prop="iterateWeek">
          <el-date-picker
            v-model="form.iterateWeek"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="选择日期"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 批量导入发布任务对话框 -->
    <el-dialog title="批量导入发布任务" :visible.sync="openImport" width="400px" append-to-body>
      <el-form ref="form" :model="formImport" :rules="rules" label-width="100px">
          <el-upload
            drag
            name="file"
            ref="importTaskUpload"
            :action="importAction"
            :auto-upload="false"
            :multiple="false"
            :data="formImport"
            :headers="importHeaders"
            :on-error="uploadError"
            :on-success="uploadSuccess"
            :on-change="selectFileChange">
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip" slot="tip">只能上传 <b style="font-size: 15px;">xls/xlsx</b> 文件，且不超过10M</div>
          </el-upload>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitImportForm">导 入</el-button>
        <el-button @click="openImport = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 发布任务详情 -->
    <el-dialog
      title="发布详情"
      :visible.sync="openDetail"
      :before-close="closeDeployLog"
      width="40%">
      <el-collapse v-model="deployLogsActive">
        <el-collapse-item v-for="(item, idx) in deployLogs" :title="item.createTime" :name="idx">
          <template slot="title">
            {{ parseTime(item.createTime, '{y}-{m}-{d} {h}:{i}:{s}')}}
            <dict-tag :options="dict.type.deploy_status" :value="item.status"/>
          </template>
          <pre>{{ item.taskLog }}</pre>
        </el-collapse-item>
      </el-collapse>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="closeDeployLog">关 闭</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script>
import { listTask, saveDll, addTask, doDeploy, deployDetail } from "@/api/deploy";
import {getToken} from "@/utils/auth";
import {parseTime} from "@/utils/ruoyi";

export default {
  name: "Deploy",
  dicts: ['deploy_status', 'out_dll', 'deploy_task_type'],
  data() {
    return {
      // 遮罩层
      loading: true,
      fullscreenLoading: false,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 表格数据
      taskList: [],
      selectList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      openImport: false,
      openDetail: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        demandName: undefined,
        dateRange: [],
        iterateWeek: undefined,
        status: undefined
      },
      env: "",
      // 批量导入发布任务表单参数
      importHeaders: {
        Authorization: "Bearer " + getToken(),
      },
      importAction: process.env.VUE_APP_BASE_API + "/deploy/importTask",
      formImport: {
        env: ""
      },
      // 新增发布任务表单参数
      form: {
        jiraNo: "",
        demandName: "",
        demandType: "1",
        relateDemand: "",
        principal: "",
        iterateWeek: null,
        dlls: [],
        outDlls: "",
        remark: ""
      },
      // 新增发布任务表单校验
      rules: {
        jiraNo: [
          { required: true, message: "JIRA编号不能为空", trigger: "blur" },
          { max: 20, message: 'JIRA编号不能超过 20 个字符', trigger: 'blur' }
        ],
        demandName: [
          { required: true, message: "需求名不能为空", trigger: "blur" },
          { max: 100, message: '需求名不能超过 100 个字符', trigger: 'blur' }
        ],
        demandType: [{ required: true, message: "类型不能为空", trigger: "blur" }],
        principal: [
          { required: true, message: "责任人不能为空", trigger: "blur" },
          { max: 50, message: '责任人不能超过 50 个字符', trigger: 'blur' }
        ],
        iterateWeek: [{ required: true, message: "需求迭代周不能为空", trigger: "blur" }],
        relateDemand: [{ max: 100, message: '关联业务需求不能超过 100 个字符', trigger: 'blur' }],
        remark: [{ max: 200, message: '备注不能超过 200 个字符', trigger: 'blur' }]
      },
      deployLogsActive: [],
      deployLogs: []
    }
  },
  created() {
    // this.loading = false;
    // console.log(this.$route.meta.title)
    this.env = this.$route.meta.title
    this.formImport.env = this.$route.meta.title
    this.getList();
  },
  methods: {
    /** 查询角色列表 */
    getList() {
      let that = this
      this.loading = true;
      this.queryParams.env = this.env
      listTask(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
          this.taskList = response.rows;
          this.total = response.total;
          this.taskList.forEach(e => {
            that.$set(e, 'status', String(e.status))
            that.$set(e, 'editDlls', false)
            if (e.outDlls) {
              that.$set(e, 'dlls', e.outDlls.split(","))
            }
          });
          this.loading = false;
        }
      );
    },
    /** 保存更新dll */
    saveDll(row) {
      if (!row.dlls || row.dlls.length === 0) {
        this.$message.error('请选择DLL文件')
        return
      }
      saveDll(row).then(resp => {
        row.editDlls = !row.editDlls
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.open = true;
      this.title = "新建发布任务";
    },
    /** 提交按钮 */
    submitForm: function() {
      let that = this
      this.$refs["form"].validate(valid => {
        if (valid) {
          that.form.env = that.env
          that.form.outDlls = that.form.dlls.join(",")
          addTask(that.form).then(resp => {
            if (resp.code === 200) {
              that.open = false
              that.resetForm()
              that.getList()
            }
          })
        }
      });
    },
    resetForm() {
      this.form = {
        jiraNo: "",
        demandName: "",
        demandType: "1",
        relateDemand: "",
        principal: "",
        iterateWeek: null,
        dlls: [],
        outDlls: "",
        remark: ""
      }
    },
    selectFileChange(file) {
      const isJPG = file.name.endsWith("xls") || file.name.endsWith("xlsx")
      const isLt_10M = file.size / 1024 / 1024 < 10
      if (!isJPG) {
        this.$message.error('上传文件只能是 xls/xlsx 格式！')
        this.$refs.importTaskUpload.clearFiles()
      }
      if (!isLt_10M) {
        this.$message.error('上传文件大小不能超过 10MB！')
        this.$refs.importTaskUpload.clearFiles()
      }
      return isJPG && isLt_10M
    },
    submitImportForm() {
      let uploadFile = this.$refs.importTaskUpload.uploadFiles
      if (!uploadFile || uploadFile.length < 1) {
        this.$message.warning('请选择文件！')
      }
      this.$refs.importTaskUpload.submit()
    },
    uploadSuccess(resp, file) {
      if (resp.code !== 200) {
        this.$message.error(resp.msg)
        this.$refs.importTaskUpload.uploadFiles[0].status = "ready"
      } else {
        this.$message.success('导入成功！')
        this.openImport = false
        this.getList()
        this.$refs.importTaskUpload.clearFiles()
      }
    },
    uploadError(err, file) {
      this.$message.error('导入失败！')
    },
    doDeploy(data) {
      this.$confirm('确认发布?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const loading = this.$loading({
          lock: true,
          text: '发布中...',
          spinner: 'el-icon-loading',
          background: 'rgba(255, 255, 255, 0.7)'
        })
        doDeploy([data.id], data.env).then(resp => {
          if (resp.code === 200) {
            this.$message.success('发布成功！')
          } else {
            this.$message.error('发布失败！')
          }
          loading.close()
        })
      }).catch(() => {
      })
    },
    deployDetail(data) {
      deployDetail(data).then(resp => {
        if (resp.code === 200) {
          this.deployLogs = resp.data
          this.openDetail = true
        } else {
          this.$message.error('查询失败！')
        }
      })
    },
    closeDeployLog() {
      this.deployLogsActive = []
      this.openDetail = false
    },
    /** 一键批量发布 */
    batchDeploy() {
      let ids = this.selectList.map(e => e.id);
      if (!ids || ids.length === 0) {
        return
      }
      this.$confirm('确认发布?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const loading = this.$loading({
          lock: true,
          text: '发布中...',
          spinner: 'el-icon-loading',
          background: 'rgba(255, 255, 255, 0.7)'
        })
        doDeploy(ids, this.env).then(resp => {
          debugger
          if (resp.code === 200) {
            this.$message.success('发布成功！')
          } else {
            this.$message.error('发布失败！')
          }
          loading.close()
        }).catch(err => {
          console.log(err)
          loading.close()
        })
      }).catch(() => {
      })
    },
    taskSelect(val) {
      this.selectList = val
    }
  }
};
</script>
