<template>
  <div>
    <!-- <div
      v-if=" !item.meta || !item.meta.hidden "
      :class="['menu-wrapper', isCollapse ? 'simple-mode' : 'full-mode', {'first-level': isFirstLevel}]"
    > -->
    <div
      v-if="!item.meta || !item.meta.hidden"
      :class="['menu-wrapper', 'full-mode', { 'first-level': isFirstLevel }]"
    >
      <template v-if="theOnlyOneChild && !theOnlyOneChild.children">
        <sidebar-item-link
          v-if="theOnlyOneChild.meta"
          :to="resolvePath(theOnlyOneChild.path)"
        >
          <el-menu-item
            :index="resolvePath(theOnlyOneChild.path)"
            :class="{ 'submenu-title-noDropdown': isFirstLevel }"
          >
            <!-- <i v-if="theOnlyOneChild.meta.title==='工作台'" class="iconfont icon img-icon-sel" /> -->
            <!-- <svg-icon v-if="theOnlyOneChild.meta.title==='工作台'" name="dashboard" width="20" height="20"></svg-icon> -->
            <img
              v-if="theOnlyOneChild.meta.icon"
              class="fresh-menu-icon"
              :src="menuIcon(theOnlyOneChild.meta.icon)"
              alt=""
            />
            <span v-if="theOnlyOneChild.meta.title" slot="title">{{
              theOnlyOneChild.meta.title
            }}</span>
          </el-menu-item>
        </sidebar-item-link>
      </template>
      <el-submenu v-else :index="resolvePath(item.path)" popper-append-to-body>
        <template slot="title">
          <img
            v-if="item.meta && item.meta.icon"
            class="fresh-menu-icon"
            :src="menuIcon(item.meta.icon)"
            alt=""
          />
          <span v-if="item.meta && item.meta.title" slot="title">{{
            item.meta.title
          }}</span>
        </template>
        <template v-if="item.children">
          <sidebar-item
            v-for="child in item.children"
            :key="child.path"
            :item="child"
            :is-collapse="isCollapse"
            :is-first-level="false"
            :base-path="resolvePath(child.path)"
            class="nest-menu"
          />
        </template>
      </el-submenu>
    </div>
  </div>
</template>

<script lang="ts">
import path from 'path'
import { Component, Prop, Vue } from 'vue-property-decorator'
import { UserModule } from '@/store/modules/user'
import { Route, RouteConfig } from 'vue-router'
import { isExternal } from '@/utils/validate'
import SidebarItemLink from './SidebarItemLink.vue'

@Component({
  name: 'SidebarItem',
  components: {
    SidebarItemLink,
  },
})
export default class extends Vue {
  @Prop({ required: true }) private item!: RouteConfig
  @Prop({ default: false }) private isCollapse!: boolean
  @Prop({ default: true }) private isFirstLevel!: boolean
  @Prop({ default: '' }) private basePath!: string

  get showingChildNumber() {
    if (this.item.children) {
      const showingChildren = this.item.children.filter((item) => {
        if (item.meta && item.meta.hidden) {
          return false
        }
        return true
      })
      return showingChildren.length
    }
    return 0
  }

  get roles() {
    return UserModule.roles
  }

  get theOnlyOneChild() {
    if (this.showingChildNumber > 0) {
      return null
    }
    if (this.item.children) {
      for (let child of this.item.children) {
        if (!child.meta || !child.meta.hidden) {
          return child
        }
      }
    }
    // If there is no children, return itself with path removed,
    // because this.basePath already conatins item's path information
    return { ...this.item, path: '' }
  }

  private resolvePath(routePath: string) {
    if (isExternal(routePath)) {
      return routePath
    }
    if (isExternal(this.basePath)) {
      return this.basePath
    }
    return path.resolve(this.basePath, routePath)
  }

  private menuIcon(icon: string) {
    const icons: { [key: string]: string } = {
      dashboard: 'house',
      'icon-dish': 'shopping-basket',
      'icon-category': 'layout-grid',
      'icon-order': 'clipboard-list',
      'icon-combo': 'users',
      'icon-statistics': 'chart-column',
    }
    return '/branding/icons/' + (icons[icon] || 'layout-grid') + '.svg'
  }
}
</script>
<style scoped>
.fresh-menu-icon { display: inline-block; width: 20px; height: 20px; margin-right: 12px; vertical-align: middle; opacity: .9; flex-shrink: 0; }
</style>
