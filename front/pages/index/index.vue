<template><Catalog ref="catalog" /></template>
<script>
import Catalog from "../../components/Catalog.vue";
export default {
  components: { Catalog },
  onShow() { this.$refs.catalog?.load(); }
};
</script>
