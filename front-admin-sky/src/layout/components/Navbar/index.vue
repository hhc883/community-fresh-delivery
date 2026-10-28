<template>
  <div class="navbar">
    <div class="statusBox">
      <hamburger
        id="hamburger-container"
        :is-active="sidebar.opened"
        class="hamburger-container"
        @toggleClick="toggleSideBar"
      />
      <div class="header-title">
        <span class="platform-name">社区鲜选</span>
        <span class="platform-divider">/</span>
        <span class="current-module">{{ currentModuleTitle }}</span>
      </div>
      <div class="state-pill" :class="status === 1 ? 'is-open' : 'is-closed'">
        <span class="dot" />
        <span class="text">{{ status === 1 ? '生鲜服务营业中' : '生鲜服务打烊中' }}</span>
      </div>
    </div>

    <div :key="restKey" class="right-menu">
      <div class="rightStatus">
        <audio ref="audioVo" hidden>
          <source src="./../../../assets/preview.mp3" type="audio/mp3" />
        </audio>
        <audio ref="audioVo2" hidden>
          <source src="./../../../assets/reminder.mp3" type="audio/mp3" />
        </audio>
        <div class="nav-action-btn" @click="handleStatus">
          <i class="el-icon-time action-icon" />
          <span>营业状态设置</span>
        </div>
      </div>

      <div class="avatar-wrapper">
        <div
          :class="shopShow ? 'userInfo' : ''"
          class="user-trigger"
          @mouseenter="toggleShow"
          @mouseleave="mouseLeaves"
        >
          <div class="user-chip">
            <span class="user-avatar-tag">管</span>
            <span class="user-name">{{ name }}</span>
            <i class="el-icon-arrow-down" :class="{ 'is-active': shopShow }" />
          </div>
          <div v-if="shopShow" class="userList">
            <p class="amendPwdIcon" @click="handlePwd">
              <i class="el-icon-lock" />
              <span>修改密码</span>
            </p>
            <p class="outLogin" @click="logout">
              <i class="el-icon-switch-button" />
              <span>退出登录</span>
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 营业状态弹层 -->
    <el-dialog
      title="生鲜服务营业状态设置"
      :visible.sync="dialogVisible"
      width="440px"
      :show-close="true"
    >
      <div class="status-setting-content">
        <el-radio-group v-model="setStatus" class="status-radio-group">
          <el-radio :label="1" class="status-radio-item">
            <div class="radio-title">营业中</div>
            <div class="radio-desc">
              当前社区生鲜服务处于对外营业展示状态，支持自提与配送订单流转。
            </div>
          </el-radio>
          <el-radio :label="0" class="status-radio-item">
            <div class="radio-title">打烊中</div>
            <div class="radio-desc">
              当前社区生鲜服务处于打烊歇业展示状态，提示顾客在营业时段选购。
            </div>
          </el-radio>
        </el-radio-group>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSave">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 修改密码 -->
    <Password
      :dialog-form-visible="dialogFormVisible"
      @handleclose="handlePwdClose"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator'
import { AppModule } from '@/store/modules/app'
import { UserModule } from '@/store/modules/user'
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import Hamburger from '@/components/Hamburger/index.vue'
import { getStatus, setStatus } from '@/api/users'
import Cookies from 'js-cookie'
import { debounce, throttle } from '@/utils/common'
import { setNewData, getNewData } from '@/utils/cookies'

// 接口
import { getCountUnread } from '@/api/inform'
// 修改密码弹层
import Password from '../components/password.vue'

@Component({
  name: 'Navbar',
  components: {
    Breadcrumb,
    Hamburger,
    Password,
  },
})
export default class extends Vue {
  get currentModuleTitle() {
    return (this.$route.meta && this.$route.meta.title) || '运营管理'
  }

  private storeId = this.getStoreId
  private restKey: number = 0
  private websocket = null
  private newOrder = ''
  private message = ''
  private audioIsPlaying = false
  private audioPaused = false
  private statusValue = true
  private audioUrl: './../../../assets/preview.mp3'
  private shopShow = false
  private dialogVisible = false
  private status = 1
  private setStatus = 1
  private dialogFormVisible = false
  private ountUnread = 0
  // get ountUnread() {
  //   return Number(getNewData())
  // }
  get sidebar() {
    return AppModule.sidebar
  }

  get device() {
    return AppModule.device.toString()
  }

  getuserInfo() {
    return UserModule.userInfo
  }

  get name() {
    return (UserModule.userInfo as any).name
      ? (UserModule.userInfo as any).name
      : JSON.parse(Cookies.get('user_info') as any).name
  }

  get getStoreId() {
    let storeId = ''
    if (UserModule.storeId) {
      storeId = UserModule.storeId
    } else if ((UserModule.userInfo as any).stores != null) {
      storeId = (UserModule.userInfo as any).stores[0].storeId
    }
    return storeId
  }
  mounted() {
    document.addEventListener('click', this.handleClose)
    //console.log(this.$store.state.app.statusNumber)
    // const msg = {
    //   data: {
    //     type: 2,
    //     content: '订单1653904906519客户催单，已下单23分钟，仍未接单。',
    //     details: '434'
    //   }
    // }
    this.getStatus()
  }
  created() {
    if (process.env.VUE_APP_SOCKET_URL) this.webSocket()
  }
  onload() {
  }
  destroyed() {
    if (this.websocket) this.websocket.close() //离开路由之后断开websocket连接
  }

  // 添加新订单提示弹窗
  webSocket() {
    const that = this as any
    let clientId = Math.random().toString(36).substr(2)
    let socketUrl = (process.env.VUE_APP_SOCKET_URL || ((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.hostname + ':8080/ws/')) + clientId
    console.log(socketUrl, 'socketUrl')
    if (typeof WebSocket == 'undefined') {
      that.$notify({
        title: '提示',
        message: '当前浏览器无法接收实时报警信息，请使用谷歌浏览器！',
        type: 'warning',
        duration: 0,
      })
    } else {
      this.websocket = new WebSocket(socketUrl)
      // 监听socket打开
      this.websocket.onopen = function () {
        console.log('浏览器WebSocket已打开')
      }
      // 监听socket消息接收
      this.websocket.onmessage = function (msg) {
        // 转换为json对象
        that.$refs.audioVo.currentTime = 0
        that.$refs.audioVo2.currentTime = 0

        console.log(msg, JSON.parse(msg.data), 'msg')
        // const h = this.$createElement
        const jsonMsg = JSON.parse(msg.data)
        if (jsonMsg.type === 1) {
          that.$refs.audioVo.play()
        } else if (jsonMsg.type === 2) {
          that.$refs.audioVo2.play()
        }
        that.$notify({
          title: jsonMsg.type === 1 ? '待接单' : '催单',
          duration: 0,
          dangerouslyUseHTMLString: true,
          onClick: () => {
            that.$router
              .push(`/order?orderId=${jsonMsg.orderId}`)
              .catch((err) => {
                console.log(err)
              })
            setTimeout(() => {
              location.reload()
            }, 100)
          },
          // 这里也可以把返回信息加入到message中显示
          message: `${
            jsonMsg.type === 1
              ? `<span>您有1个<span style=color:#419EFF>订单待处理</span>,${jsonMsg.content},请及时接单</span>`
              : `${jsonMsg.content}<span style='color:#419EFF;cursor: pointer'>去处理</span>`
          }`,
        })
      }
      // 监听socket错误
      this.websocket.onerror = function () {
        that.$notify({
          title: '错误',
          message: '服务器错误，无法接收实时报警信息',
          type: 'error',
          duration: 0,
        })
      }
      // 监听socket关闭
      this.websocket.onclose = function () {
        console.log('WebSocket已关闭')
      }
    }
  }

  private toggleSideBar() {
    AppModule.ToggleSideBar(false)
  }
  // 退出
  private async logout() {
    this.$store.dispatch('LogOut').then(() => {
      // location.href = '/'
      this.$router.replace({ path: '/login' })
    })
    // this.$router.push(`/login?redirect=${this.$route.fullPath}`)
  }
  // 获取未读消息
  async getCountUnread() {
    const { data } = await getCountUnread()
    if (data.code === 1) {
      // this.ountUnread = data.data
      AppModule.StatusNumber(data.data)
      // setNewData(data.data)
      // this.$message.success('操作成功！')
    } else {
      this.$message.error(data.msg)
    }
  }
  // 营业状态
  async getStatus() {
    const { data } = await getStatus()
    this.status = data.data
    this.setStatus = this.status
  }
  // 下拉菜单显示
  toggleShow() {
    this.shopShow = true
  }
  // 下拉菜单隐藏
  mouseLeaves() {
    this.shopShow = false
  }
  // 触发空白处下来菜单关闭
  handleClose() {
    // clearTimeout(this.leave)
    // this.shopShow = false
  }
  // 设置营业状态
  handleStatus() {
    this.dialogVisible = true
  }
  // 营业状态设置
  async handleSave() {
    const { data } = await setStatus(this.setStatus)
    if (data.code === 1) {
      this.dialogVisible = false
      this.getStatus()
    }
  }
  // 修改密码
  handlePwd() {
    this.dialogFormVisible = true
  }
  // 关闭密码编辑弹层
  handlePwdClose() {
    this.dialogFormVisible = false
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 60px;
  position: relative;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px 0 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);

  .statusBox {
    height: 100%;
    align-items: center;
    display: flex;
  }

  .hamburger-container {
    padding: 0 16px 0 20px;
    cursor: pointer;
    transition: background 0.2s;
    height: 100%;
    display: flex;
    align-items: center;

    &:hover {
      background: rgba(0, 0, 0, 0.03);
    }
  }

  .header-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    margin-right: 18px;

    .platform-name {
      color: #64748B;
      font-weight: 500;
    }
    .platform-divider {
      color: #CBD5E1;
    }
    .current-module {
      color: #1A202C;
      font-weight: 600;
      font-size: 15px;
    }
  }

  .state-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
    line-height: 1.4;

    .dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
    }

    &.is-open {
      background-color: #E8F5E9;
      color: #27744D;
      border: 1px solid #A5D6A7;
      .dot {
        background-color: #27744D;
        box-shadow: 0 0 0 2px rgba(39, 116, 77, 0.2);
      }
    }

    &.is-closed {
      background-color: #F1F5F9;
      color: #64748B;
      border: 1px solid #CBD5E1;
      .dot {
        background-color: #94A3B8;
      }
    }
  }

  .right-menu {
    display: flex;
    align-items: center;
    gap: 16px;
    font-size: 13px;

    .nav-action-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      padding: 6px 12px;
      border-radius: 6px;
      border: 1px solid #E2E8F0;
      background: #FAFAFA;
      color: #475569;
      transition: all 0.2s;

      .action-icon {
        color: #27744D;
        font-size: 15px;
      }

      &:hover {
        background: #F1F5F9;
        color: #1A202C;
        border-color: #CBD5E1;
      }
    }
  }

  .avatar-wrapper {
    position: relative;

    .user-trigger {
      position: relative;
      cursor: pointer;
    }

    .user-chip {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px 10px;
      border-radius: 6px;
      background: #F8FAF9;
      border: 1px solid #E2E8F0;
      transition: all 0.2s;

      &:hover {
        background: #EEF5F1;
        border-color: #A5D6A7;
      }

      .user-avatar-tag {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: #27744D;
        color: #FFFFFF;
        font-size: 12px;
        font-weight: 600;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .user-name {
        color: #1A202C;
        font-weight: 500;
        max-width: 90px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .el-icon-arrow-down {
        font-size: 12px;
        color: #64748B;
        transition: transform 0.2s;

        &.is-active {
          transform: rotate(180deg);
        }
      }
    }

    .userList {
      position: absolute;
      top: 40px;
      right: 0;
      width: 130px;
      background: #FFFFFF;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
      border-radius: 6px;
      border: 1px solid #E2E8F0;
      padding: 6px 0;
      z-index: 2000;

      p {
        margin: 0;
        padding: 8px 14px;
        display: flex;
        align-items: center;
        gap: 8px;
        color: #475569;
        font-size: 13px;
        cursor: pointer;
        transition: background 0.15s;

        i {
          font-size: 14px;
          color: #64748B;
        }

        &:hover {
          background: #F8FAF9;
          color: #27744D;

          i {
            color: #27744D;
          }
        }
      }

      .outLogin:hover {
        color: #E53E3E;
        i {
          color: #E53E3E;
        }
      }
    }
  }
}

.status-setting-content {
  padding: 10px 0;

  .status-radio-group {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .status-radio-item {
    display: flex;
    align-items: flex-start;
    margin-right: 0;
    padding: 12px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    transition: all 0.2s;

    &.is-checked {
      border-color: #27744D;
      background: #F0F9F4;
    }

    .radio-title {
      font-size: 14px;
      font-weight: 600;
      color: #1A202C;
      margin-bottom: 4px;
    }

    .radio-desc {
      font-size: 12px;
      color: #64748B;
      line-height: 1.5;
      white-space: normal;
    }
  }
}
</style>
