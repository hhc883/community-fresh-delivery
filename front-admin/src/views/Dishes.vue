<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">商品管理</h2>
      <el-tag type="danger" effect="dark" size="small" class="mode-badge">本页会改数据</el-tag>
    </div>
    <p class="page-note">分类与商品均为真实数据库读写；上下架立即影响用户端可见性。数据源：GET /admin/dish/page（契约 docs/contracts/api-v1.md）。</p>

    <div class="layout">
      <!-- 分类列 -->
      <aside class="side">
        <div class="side-head">
          <span class="side-title">商品分类</span>
          <el-button size="small" type="primary" plain @click="openCategoryDialog()">新增</el-button>
        </div>
        <ul class="cat-list">
          <li
            v-for="cat in categories"
            :key="cat.id"
            :class="{ active: filter.categoryId === cat.id }"
            @click="onPickCategory(cat.id)"
          >
            <span class="cat-name">{{ cat.name }}{{ Number(cat.status) === 1 ? '' : '（禁用）' }}</span>
            <span class="cat-actions">
              <el-button link size="small" @click.stop="openCategoryDialog(cat)">编辑</el-button>
              <el-button link size="small" @click.stop="toggleCategory(cat)">{{ Number(cat.status) === 1 ? '禁用' : '启用' }}</el-button>
              <el-button link size="small" type="danger" @click.stop="onDeleteCategory(cat)">删除</el-button>
            </span>
          </li>
        </ul>
        <el-button v-if="filter.categoryId" size="small" text @click="onPickCategory(null)">显示全部分类</el-button>
      </aside>

      <!-- 商品表 -->
      <section class="main">
        <div class="toolbar">
          <el-input
            v-model="filter.name"
            placeholder="商品名称"
            clearable
            style="width: 200px"
            @keyup.enter="onSearch"
          />
          <el-select v-model="filter.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="起售" :value="1" />
            <el-option label="停售" :value="0" />
          </el-select>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button type="success" @click="openDishDialog()">新增商品</el-button>
        </div>

        <div v-if="viewState === 'loading'" class="state-box">商品列表加载中…</div>
        <div v-else-if="viewState === 'error'" class="state-box">
          <div class="state-title">读取失败</div>
          <div class="state-desc">{{ errorText }}</div>
          <el-button size="small" @click="loadDishes">重试</el-button>
        </div>
        <template v-else>
          <el-table :data="rows" border stripe @selection-change="onSelectionChange">
            <el-table-column type="selection" width="44" />
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column label="图片" width="72">
              <template #default="{ row }">
                <el-image
                  v-if="row.image"
                  :src="resolveImage(row.image)"
                  :preview-src-list="[resolveImage(row.image)]"
                  fit="cover"
                  style="width: 44px; height: 44px; border-radius: 4px"
                  preview-teleported
                />
                <span v-else class="muted">无图</span>
              </template>
            </el-table-column>
            <el-table-column label="价格" width="90">
              <template #default="{ row }">￥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="80" />
            <el-table-column prop="soldCount" label="销量" width="80" />
            <el-table-column label="团购" width="140">
              <template #default="{ row }">
                <el-tag v-if="row.isGroupon === 1" type="success" size="small">
                  ￥{{ row.grouponPrice }} × {{ row.grouponSize }}人团
                </el-tag>
                <span v-else class="muted">非团购</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '起售' : '停售' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDishDialog(row)">编辑</el-button>
                <el-button v-if="row.status === 1" link type="warning" @click="onSetStatus(row, 0)">停售</el-button>
                <el-button v-else link type="success" @click="onSetStatus(row, 1)">起售</el-button>
                <el-button link type="danger" @click="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="page"
            @current-change="onPageChange"
          />
          <el-button
            v-if="selected.length"
            class="batch-del"
            type="danger"
            plain
            size="small"
            @click="onBatchDelete"
          >
            批量删除（{{ selected.length }}）
          </el-button>
        </template>
      </section>
    </div>

    <!-- 分类编辑 -->
    <el-dialog v-model="categoryDialog.visible" :title="categoryDialog.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form label-width="70px">
        <el-form-item label="名称" required>
          <el-input v-model="categoryDialog.name" maxlength="32" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryDialog.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="categoryDialog.saving" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>

    <!-- 商品编辑 -->
    <el-dialog v-model="dishDialog.visible" :title="dishDialog.id ? '编辑商品' : '新增商品'" width="620px" top="6vh">
      <el-form label-width="92px">
        <el-form-item label="名称" required>
          <el-input v-model="dishDialog.name" maxlength="32" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="dishDialog.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="dishDialog.price" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="dishDialog.stock" :min="0" :step="10" />
          <span class="form-note">直接置值；服务端下单按库存条件扣减</span>
        </el-form-item>
        <el-form-item label="规格单位">
          <el-input v-model="dishDialog.unit" maxlength="16" placeholder="如 500g / 份 / 箱" style="width: 220px" />
        </el-form-item>
        <el-form-item label="产地">
          <el-input v-model="dishDialog.origin" maxlength="64" placeholder="如 云南" style="width: 220px" />
        </el-form-item>
        <el-form-item label="生鲜商品">
          <el-switch v-model="dishDialog.isFresh" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dishDialog.description" type="textarea" maxlength="255" :rows="2" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="dishDialog.image" placeholder="图片 URL 或上传后的地址" />
        </el-form-item>
        <el-divider>团购设置</el-divider>
        <el-form-item label="参与团购">
          <el-switch v-model="dishDialog.isGroupon" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <template v-if="dishDialog.isGroupon === 1">
          <el-form-item label="拼团价" required>
            <el-input-number v-model="dishDialog.grouponPrice" :min="0.01" :precision="2" :step="1" />
          </el-form-item>
          <el-form-item label="成团人数">
            <el-input-number v-model="dishDialog.grouponSize" :min="2" :max="99" />
          </el-form-item>
          <el-form-item label="有效时长">
            <el-input-number v-model="dishDialog.grouponValidHours" :min="1" :max="720" />
            <span class="form-note">小时；超时未成团由服务端关闭并处置订单</span>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dishDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dishDialog.saving" @click="saveDish">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 商品管理：分类列表 + 商品分页 + 新增/编辑/上下架/删除。
// DishDTO 含库存与团购字段（docs/contracts/api-v1.md §2）。
// 写操作成功与否一律以服务端返回为准：成功后重拉当前页核对，不本地改行。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BASE_URL, createCategory, createDish, deleteCategory, deleteDishes, getCategoryPage, setCategoryStatus, getDish, getDishPage, setDishStatus, updateCategory, updateDish } from '../api'

const viewState = ref('loading')
const errorText = ref('')

const categories = ref([])
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const selected = ref([])
const filter = reactive({ name: '', categoryId: null, status: null })

// 图片解析：库里的 image 可能是完整 URL 或以 / 开头的静态路径（原样用），
// 也可能是上传后的纯文件名——按后端通用下载端点拼前缀。
function resolveImage(image) {
  if (!image) return ''
  if (/^https?:\/\//.test(image) || image.startsWith('/')) return image
  return BASE_URL + '/api/common/download?name=' + encodeURIComponent(image)
}

async function loadCategories() {
  const list = []
  let categoryPage = 1
  let res
  do {
    res = await getCategoryPage({ type: 1, page: categoryPage++, pageSize: 100 })
    const records = res.data?.records || []
    list.push(...records)
    if (!records.length) break
  } while (list.length < Number(res.data?.total || 0))
  categories.value = list
}

async function toggleCategory(cat) {
  try {
    await setCategoryStatus(cat.id, Number(cat.status) === 1 ? 0 : 1)
    await loadCategories()
    ElMessage.success('分类状态已更新')
  } catch (err) { ElMessage.error(err.message || '分类状态更新失败') }
}

async function loadDishes() {
  viewState.value = 'loading'
  errorText.value = ''
  try {
    const res = await getDishPage({
      page: page.value,
      pageSize,
      name: filter.name,
      categoryId: filter.categoryId,
      status: filter.status
    })
    const data = res.data || {}
    rows.value = data.records || []
    total.value = Number(data.total) || 0
    viewState.value = 'ready'
  } catch (err) {
    const e = err || {}
    if (e.code === 401) return
    viewState.value = 'error'
    errorText.value = e.message || '商品列表读取失败'
  }
}

function onSearch() {
  page.value = 1
  loadDishes()
}
function onPickCategory(categoryId) {
  filter.categoryId = categoryId
  page.value = 1
  loadDishes()
}
function onPageChange(p) {
  page.value = p
  loadDishes()
}
function onSelectionChange(sel) {
  selected.value = sel
}

// ---- 分类 ----
const categoryDialog = reactive({ visible: false, id: null, name: '', sort: 0, saving: false })

function openCategoryDialog(cat) {
  categoryDialog.id = cat ? cat.id : null
  categoryDialog.name = cat ? cat.name : ''
  categoryDialog.sort = cat ? cat.sort : 0
  categoryDialog.visible = true
}

async function saveCategory() {
  if (categoryDialog.saving) return
  if (!categoryDialog.name.trim()) {
    ElMessage.warning('请填写分类名称')
    return
  }
  categoryDialog.saving = true
  try {
    const body = { type: 1, name: categoryDialog.name.trim(), sort: categoryDialog.sort }
    if (categoryDialog.id) {
      await updateCategory({ id: categoryDialog.id, ...body })
    } else {
      await createCategory(body)
    }
    ElMessage.success('分类已保存')
    categoryDialog.visible = false
    await loadCategories()
  } catch (err) {
    ElMessage.error((err && err.message) || '保存失败')
  } finally {
    categoryDialog.saving = false
  }
}

async function onDeleteCategory(cat) {
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${cat.name}」？\n若分类下仍有商品，服务端会拒绝删除。`,
      '删除分类',
      { type: 'warning', customClass: 'admin-write-confirm' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteCategory(cat.id)
    ElMessage.success('分类已删除')
    if (filter.categoryId === cat.id) filter.categoryId = null
    await loadCategories()
    loadDishes()
  } catch (err) {
    ElMessage.error((err && err.message) || '删除失败')
  }
}

// ---- 商品 ----
const dishDialog = reactive({
  visible: false, id: null, name: '', categoryId: null, price: null,
  stock: 0, unit: '', origin: '', description: '', image: '', flavors: [], isFresh: 1,
  isGroupon: 0, grouponPrice: null, grouponSize: 2, grouponValidHours: 24,
  saving: false
})

async function openDishDialog(row) {
  if (row) {
    try {
      const res = await getDish(row.id)
      const d = res.data || row
      Object.assign(dishDialog, {
        visible: true, id: d.id, name: d.name, categoryId: d.categoryId,
        price: d.price == null ? null : Number(d.price), stock: d.stock || 0,
        unit: d.unit || '', origin: d.origin || '',
        description: d.description || '', image: d.image || '', flavors: d.flavors || [], isFresh: Number(d.isFresh) === 1 ? 1 : 0,
        isGroupon: d.isGroupon || 0,
        grouponPrice: d.grouponPrice == null ? null : Number(d.grouponPrice),
        grouponSize: d.grouponSize || 2, grouponValidHours: d.grouponValidHours || 24
      })
    } catch (err) {
      ElMessage.error((err && err.message) || '商品详情读取失败')
    }
  } else {
    Object.assign(dishDialog, {
      visible: true, id: null, name: '', categoryId: filter.categoryId, price: null,
      stock: 0, unit: '', origin: '', description: '', image: '', flavors: [], isFresh: 1,
      isGroupon: 0, grouponPrice: null, grouponSize: 2, grouponValidHours: 24
    })
  }
}

async function saveDish() {
  if (dishDialog.saving) return
  if (!dishDialog.name.trim()) return ElMessage.warning('请填写商品名称')
  if (!dishDialog.categoryId) return ElMessage.warning('请选择分类')
  if (!dishDialog.price || dishDialog.price <= 0) return ElMessage.warning('请填写正确价格')
  if (dishDialog.isGroupon === 1 && (!dishDialog.grouponPrice || dishDialog.grouponPrice <= 0)) {
    return ElMessage.warning('参与团购必须填写拼团价')
  }
  dishDialog.saving = true
  try {
    const body = {
      id: dishDialog.id || undefined,
      name: dishDialog.name.trim(),
      categoryId: dishDialog.categoryId,
      price: dishDialog.price,
      stock: dishDialog.stock,
      isFresh: dishDialog.isFresh,
      flavors: dishDialog.flavors,
      unit: dishDialog.unit || undefined,
      origin: dishDialog.origin || undefined,
      description: dishDialog.description || undefined,
      image: dishDialog.image || undefined,
      isGroupon: dishDialog.isGroupon,
      grouponPrice: dishDialog.isGroupon === 1 ? dishDialog.grouponPrice : undefined,
      grouponSize: dishDialog.isGroupon === 1 ? dishDialog.grouponSize : undefined,
      grouponValidHours: dishDialog.isGroupon === 1 ? dishDialog.grouponValidHours : undefined,
      status: dishDialog.id ? undefined : 1
    }
    if (dishDialog.id) {
      await updateDish(body)
    } else {
      await createDish(body)
    }
    ElMessage.success('商品已保存')
    dishDialog.visible = false
    await loadDishes()
  } catch (err) {
    ElMessage.error((err && err.message) || '保存失败')
  } finally {
    dishDialog.saving = false
  }
}

async function onSetStatus(row, status) {
  const action = status === 1 ? '起售' : '停售'
  try {
    await ElMessageBox.confirm(
      `确定${action}商品「${row.name}」？\n${status === 1 ? '上架后用户端立即可见、可下单。' : '下架后用户端不可见、不可下单；已有订单不受影响。'}`,
      action + '商品',
      { type: 'warning', customClass: 'admin-write-confirm' }
    )
  } catch (e) {
    return
  }
  try {
    await setDishStatus(row.id, status)
    ElMessage.success('状态已更新')
    await loadDishes()
  } catch (err) {
    ElMessage.error((err && err.message) || '操作失败')
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除商品「${row.name}」？\n删除后用户端不再展示；若商品关联进行中的业务，服务端会拒绝删除。`,
      '删除商品',
      { type: 'warning', customClass: 'admin-write-confirm' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteDishes([row.id])
    ElMessage.success('商品已删除')
    await loadDishes()
  } catch (err) {
    ElMessage.error((err && err.message) || '删除失败')
  }
}

async function onBatchDelete() {
  const names = selected.value.map((r) => r.name).join('、')
  try {
    await ElMessageBox.confirm(
      `确定删除以下 ${selected.value.length} 个商品？\n${names}`,
      '批量删除商品',
      { type: 'warning', customClass: 'admin-write-confirm' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteDishes(selected.value.map((r) => r.id))
    ElMessage.success('商品已删除')
    await loadDishes()
  } catch (err) {
    ElMessage.error((err && err.message) || '删除失败')
  }
}

onMounted(async () => {
  try {
    await loadCategories()
  } catch (e) {
    // 分类读取失败不阻塞商品列表：商品表仍可查询
    ElMessage.error(e.message || '分类读取失败，请刷新重试')
  }
  loadDishes()
})
</script>

<style scoped>
.layout {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  align-items: flex-start;
}
.side {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  padding: 12px;
}
.side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.side-title {
  font-weight: 600;
  font-size: 14px;
}
.cat-list {
  list-style: none;
  margin: 0 0 8px;
  padding: 0;
}
.cat-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}
.cat-list li:hover {
  background: #f5f7fa;
}
.cat-list li.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}
.cat-actions {
  visibility: hidden;
}
.cat-list li:hover .cat-actions {
  visibility: visible;
}
.main {
  flex: 1;
  min-width: 0;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.batch-del {
  margin-top: 14px;
}
.muted {
  color: #c0c4cc;
  font-size: 12px;
}
.form-note {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
.state-box {
  padding: 40px;
  text-align: center;
  background: #fff;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
}
.state-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}
.state-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
}
</style>
