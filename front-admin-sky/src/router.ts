import Vue from "vue";
import Router from "vue-router";
import Layout from "@/layout/index.vue";
Vue.use(Router);
const extension = () => import("@/views/extensions/index.vue");
export default new Router({
 scrollBehavior: (_, __, saved) => saved || {x:0,y:0}, base:process.env.BASE_URL,
 routes:[
  {path:"/login",component:()=>import("@/views/login/index.vue"),meta:{title:"社区生鲜管理",hidden:true,notNeedAuth:true}},
  {path:"/404",component:()=>import("@/views/404.vue"),meta:{title:"页面不存在",hidden:true,notNeedAuth:true}},
  {path:"/",component:Layout,redirect:"/dashboard",children:[
   {path:"dashboard",name:"Dashboard",component:extension,meta:{title:"工作台",icon:"dashboard",extension:"overview"}},
   {path:"dish",component:extension,meta:{title:"商品管理",icon:"icon-dish",extension:"dishes"}},
   {path:"category",component:()=>import("@/views/category/index.vue"),meta:{title:"分类管理",icon:"icon-category"}},
   {path:"order",component:extension,meta:{title:"订单管理",icon:"icon-order",extension:"orders"}},
   {path:"groupon",component:extension,meta:{title:"社区拼团",icon:"icon-combo",extension:"groupon"}},
   {path:"statistics",component:extension,meta:{title:"数据统计",icon:"icon-statistics",extension:"reports"}}
  ]},
  ...Object.entries({'/fresh-products':'/dish','/dish/add':'/dish','/fulfillment':'/order','/group-reports':'/statistics','/employee':'/dashboard','/employee/add':'/dashboard','/setmeal':'/dish','/setmeal/add':'/dish'}).map(([path,redirect])=>({path,redirect,meta:{hidden:true}})),
  {path:"*",redirect:"/404",meta:{hidden:true}}
 ]
});
