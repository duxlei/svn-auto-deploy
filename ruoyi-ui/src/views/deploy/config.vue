<template>
  <div class="deploy-config">
    <el-form ref="form" :model="form" label-width="160px">
      <el-form-item label="SVN仓库地址">
        <el-input v-model="form.name"></el-input>
      </el-form-item>

      <el-form-item label="通知邮件列表">
        <el-tag
          :key="tag"
          v-for="tag in dynamicTags"
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

      <el-form-item label="Excel导入跳过N行">
        <el-input-number v-model="form.excelSkipRow" :min="1" :max="10"></el-input-number>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="onSubmit">保存配置</el-button>
        <el-button>取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: "config",
  data() {
    return {
      form: {
        excelSkipRow: 3,
        name: '',
        region: '',
        date1: '',
        date2: '',
        delivery: false,
        type: [],
        resource: '',
        desc: ''
      },
      dynamicTags: ['zhangsan@svn.com', 'lisi@svn.com', 'wangwu@svn.com'],
      inputVisible: false,
      inputValue: ''
    }
  },
  methods: {
    onSubmit() {
      console.log('submit!');
    },

    handleClose(tag) {
      this.dynamicTags.splice(this.dynamicTags.indexOf(tag), 1);
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
        this.dynamicTags.push(inputValue);
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
    padding: 50px 30px;
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
