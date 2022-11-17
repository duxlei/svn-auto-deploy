<template>
  <div class="deploy-config">
    <el-form ref="form" :model="form" :rules="rules" label-width="160px">

      <el-divider content-position="center">基础配置</el-divider>
      <br/>

      <el-form-item label="SVN仓库地址" prop="svnUrl">
        <el-input v-model="form.svnUrl" placeholder="192.168.56.100">
          <template slot="prepend">svn://</template>
        </el-input>
      </el-form-item>
      <el-form-item label="通知邮件列表">
        <el-tag
          :key="tag"
          v-for="tag in emailList"
          closable
          :disable-transitions="false"
          @close="handleClose(tag)">
          {{tag}}
        </el-tag>
        <el-input
          class="input-new-tag"
          v-if="inputVisible"
          v-model="inputValue"
          ref="saveTagInput"
          size="medium"
          @keyup.enter.native="handleInputConfirm"
          @blur="handleInputConfirm"
        >
        </el-input>
        <el-button v-else class="button-new-tag" size="small" @click="showInput">+ 添加邮箱</el-button>
      </el-form-item>

      <el-form-item label="Excel导入跳过N行" prop="excelSkipRow">
        <el-input-number v-model="form.excelSkipRow" :min="0" :max="100"></el-input-number>
      </el-form-item>

      <br/>
      <el-divider content-position="center">邮件发送配置</el-divider>
      <br/>
      <el-form-item label="邮件服务器" prop="mail.host">
        <el-input v-model="form.mail.host" placeholder="smtp.163.com" />
      </el-form-item>
      <el-form-item label="端口" prop="mail.port">
        <el-input v-model="form.mail.port" placeholder="25" />
      </el-form-item>
      <el-form-item label="发送人" prop="mail.port">
        <el-input v-model="form.mail.from" placeholder="svndeploy@camp.com" />
      </el-form-item>
      <el-form-item label="授权密码" prop="mail.pass">
        <el-input v-model="form.mail.pass" type="password" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="onSubmit">保存配置</el-button>
        <el-button>取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import {saveConfig, getConfig} from "@/api/deploy";

export default {
  name: "config",
  data() {
    return {
      form: {
        excelSkipRow: 3,
        svnUrl: '',
        mail: {
          host: '',
          port: '',
          from: '',
          pass: ''
        }
      },
      rules: {
        svnUrl: [
          { required: true, message: "SVN地址不能为空", trigger: "blur" },
          { max: 200, message: 'SVN地址不能超过 200 个字符', trigger: 'blur' },
          { pattern: /^(([1-9]?\d|1\d{2}|2[0-4]\d|25[0-5])\.){3}([1-9]?\d|1\d{2}|2[0-4]\d|25[0-5])(\/[\w\-_]*)*\/?$/, message: 'SVN地址格式错误', trigger: 'blur' }
        ],
        excelSkipRow: [
          { required: true, message: "Excel导入跳过行不能为空", trigger: "blur" }
        ],
      },
      emailList: [],
      inputVisible: false,
      inputValue: ''
    }
  },
  created() {
    getConfig().then(resp => {
      if (resp.code === 200) {
        this.form.svnUrl = resp.data.svnUrl.replaceAll("svn://", "")
        this.form.excelSkipRow = resp.data.excelSkipRow
        this.form.mail = resp.data.mail
        this.emailList = resp.data.notifyEmails ? resp.data.notifyEmails.split(',') : []
      }
    })
  },
  methods: {
    onSubmit() {
      // if (!this.form.mail || !this.form.mail.host || !this.form.mail.port || !this.form.mail.from || !this.form.mail.pass) {
      //   this.$message.error('请补全邮件发送配置！')
      //   return
      // }
      this.$refs["form"].validate(valid => {
        if (valid) {
          let param = {
            svnUrl: 'svn://' + this.form.svnUrl,
            excelSkipRow: this.form.excelSkipRow,
            mail: this.form.mail,
            notifyEmails: this.emailList.join(',')
          }
          saveConfig(param).then(resp => {
            if (resp.code === 200) {
              this.$message({
                message: '保存成功',
                type: 'success'
              })
            } else {
              this.$message.error(resp.msg);
            }
          })
        }
      })
    },

    handleClose(tag) {
      this.emailList.splice(this.emailList.indexOf(tag), 1);
    },

    showInput() {
      this.inputVisible = true;
      this.$nextTick(_ => {
        this.$refs.saveTagInput.$refs.input.focus();
      });
    },

    handleInputConfirm() {
      let inputValue = this.inputValue;
      if (inputValue) {
        let pattern = /\w[-\w.+]*@([A-Za-z0-9]+\.)+[A-Za-z]{1,14}/
        if (pattern.test(inputValue)) {
          if (this.emailList.length >= 50) {
            this.$message('最多配置50个邮箱');
          }
          this.emailList.push(inputValue)
        } else {
          this.$message('邮箱格式错误 ' + inputValue);
        }
      }
      this.inputVisible = false;
      this.inputValue = '';
    }
  }
}
</script>

<style scoped>
  .deploy-config {
    width: 50%;
    padding: 30px;
  }

  .el-tag + .el-tag {
    margin-left: 10px;
  }
  .button-new-tag {
    margin-left: 10px;
    height: 32px;
    line-height: 30px;
    padding-top: 0;
    padding-bottom: 0;
  }
  .input-new-tag {
    width: 90px;
    margin-left: 10px;
    vertical-align: bottom;
  }
</style>
