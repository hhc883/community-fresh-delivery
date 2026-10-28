<template>
  <div class="sidebar-wrapper">
    <div class="brand-header">
      <div v-if="!isCollapse" class="brand-logo-full">
        <img src="/branding/fresh-mark.svg" alt="社区鲜选" class="brand-icon" />
        <div class="brand-text">
          <span class="brand-title">社区鲜选</span>
          <span class="brand-sub">生鲜运营管理</span>
        </div>
      </div>
      <div v-else class="brand-logo-mini">
        <img src="/branding/fresh-mark.svg" alt="社区鲜选" class="brand-icon-mini" />
      </div>
    </div>
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-openeds="defOpen"
        :default-active="defAct"
        :collapse="isCollapse"
        :background-color="variables.menuBg"
        :text-color="variables.menuText"
        :active-text-color="variables.menuActiveText"
        :unique-opened="false"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item
          v-for="route in routes"
          :key="route.path"
          :item="route"
          :base-path="route.path"
          :is-collapse="isCollapse"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator'
import { AppModule } from '@/store/modules/app'
import { UserModule } from '@/store/modules/user'
import SidebarItem from './SidebarItem.vue'
import variables from '@/styles/_variables.scss'
import { getSidebarStatus, setSidebarStatus } from '@/utils/cookies'
import Cookies from 'js-cookie'
@Component({
  name: 'SideBar',
  components: {
    SidebarItem
  }
})
export default class extends Vue {
  private restKey: number = 0
  get name() {
    return (UserModule.userInfo as any).name
      ? (UserModule.userInfo as any).name
      : JSON.parse(Cookies.get('user_info') as any).name
  }
  get defOpen() {
    // const urlArr = this.$route.path.split('/')
    // const openStr = urlArr.length > 2 ? `/${urlArr[1]}` : '/'
    let path = ['/']
    this.routes.forEach((n: any, i: number) => {
      if (n.meta.roles && n.meta.roles[0] === this.roles[0]) {
        path.splice(0, 1, n.path)
      }
    })
    return path
  }

  get defAct() {
    let path = this.$route.path
    return path
  }

  get sidebar() {
    return AppModule.sidebar
  }

  get roles() {
    return UserModule.roles
  }

  get routes() {
    let routes = JSON.parse(
      JSON.stringify([...(this.$router as any).options.routes])
    )
    console.log('-=-=routes=-=-=', routes)
    console.log('-=-=routes=-=-=', this.roles[0])
    let menuList = []
    let menu = routes.find(item => item.path === '/')
    if (menu) {
      menuList = menu.children
    }
    console.log('-=-=routes=-wwww=-=', routes)
    return menuList
  }

  get variables() {
    return variables
  }

  get isCollapse() {
    return !this.sidebar.opened
  }
  private async logout() {
    this.$store.dispatch('LogOut').then(() => {
      // location.href = '/'
      this.$router.replace({ path: '/login' })
    })
    // this.$router.push(`/login?redirect=${this.$route.fullPath}`)
  }
}
</script>

<style lang="scss" scoped>
.sidebar-wrapper {
  height: 100%;
  background-color: #132B1E;
  display: flex;
  flex-direction: column;
}

.brand-header {
  height: 60px;
  background-color: #0E2217;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  padding: 0 16px;
  user-select: none;
  overflow: hidden;
}

.brand-logo-full {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;

  .brand-icon {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
    border-radius: 8px;
  }

  .brand-text {
    display: flex;
    flex-direction: column;
    line-height: 1.2;

    .brand-title {
      color: #FFFFFF;
      font-size: 16px;
      font-weight: 600;
      letter-spacing: 0.5px;
    }

    .brand-sub {
      color: #8CCAA7;
      font-size: 11px;
      font-weight: 400;
      margin-top: 2px;
      letter-spacing: 0.2px;
    }
  }
}

.brand-logo-mini {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;

  .brand-icon-mini {
    width: 30px;
    height: 30px;
    border-radius: 8px;
  }
}

.el-scrollbar {
  flex: 1;
  background-color: #132B1E;
}

.el-menu {
  border: none;
  width: 100% !important;
  padding: 16px 8px 24px;
}
</style>

<style lang="scss">
.sidebar-container {
  .el-menu {
    background-color: #132B1E !important;

    .el-menu-item {
      color: #B7D5C4 !important;
      height: 44px;
      line-height: 44px;
      padding: 0 14px !important;
      margin: 4px 0;
      border-radius: 6px;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.2s ease;

      i {
        color: inherit;
        font-size: 18px;
        margin-right: 10px;
        vertical-align: middle;
      }

      &:hover {
        color: #FFFFFF !important;
        background-color: #1A3928 !important;
      }
    }

    .router-link-active .el-menu-item,
    .el-menu-item.is-active {
      background-color: #27744D !important;
      color: #FFFFFF !important;
      font-weight: 600 !important;
      box-shadow: 0 2px 6px rgba(19, 43, 30, 0.4);

      i {
        color: #FFFFFF !important;
      }

      span {
        color: #FFFFFF !important;
      }
    }
  }
}
</style>
