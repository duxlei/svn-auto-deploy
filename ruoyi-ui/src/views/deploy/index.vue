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
          v-hasPermi="['system:role:add']"
        >新增单发布任务</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-menu"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:role:add']"
        >新增批量发布任务（导入Excel）</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="el-icon-cpu"
          size="mini"
          @click="handleAdd"
        >一键发布</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList">
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
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:role:edit']"
          >发布</el-button>
          <el-button
            size="mini"
            @click="handleUpdate(scope.row)"
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

  </div>
</template>

<script>
import { listTask, saveDll, addTask } from "@/api/deploy";

export default {
  name: "Deploy",
  dicts: ['deploy_status', 'out_dll', 'deploy_task_type'],
  data() {
    return {
      // 遮罩层
      loading: true,
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
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
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
      // 表单参数
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
      // 表单校验
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
      }
    };
  },
  created() {
    this.getList();
    // this.loading = false;
    // console.log(this.$route.meta.title)
    this.env = this.$route.meta.title
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
      this.open = false;
      this.reset();
    },
    // 取消按钮（数据权限）
    cancelDataScope() {
      this.openDataScope = false;
      this.reset();
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      // this.resetForm("queryForm");
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
    }
  }
};
</script>
