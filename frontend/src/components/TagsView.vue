<script setup lang="ts">
import { useRouter, useRoute } from "vue-router";
import { useTagsViewStore } from "@/stores/tagsView";

const router = useRouter();
const route = useRoute();
const tagsViewStore = useTagsViewStore();

function closeTag(name: string) {
  tagsViewStore.removeView(name);
  if (route.name === name) {
    const remaining = tagsViewStore.visitedViews;
    if (remaining.length > 0) {
      router.push(remaining[remaining.length - 1].path);
    }
  }
}
</script>

<template>
  <div class="tags-view" v-if="tagsViewStore.visitedViews.length > 0">
    <div
      v-for="tag in tagsViewStore.visitedViews"
      :key="tag.name"
      class="tag-item"
      :class="{ active: route.name === tag.name }"
      @click="router.push(tag.path)"
    >
      <span>{{ tag.title }}</span>
      <el-icon class="tag-close" @click.stop="closeTag(tag.name)" v-if="tag.name !== 'Dashboard'">
        <Close />
      </el-icon>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.tags-view {
  height: $tagsview-height;
  display: flex;
  align-items: center;
  padding: 0 12px;
  background: $bg-page;
  border-bottom: 1px solid $border-light;
  gap: 4px;
  flex-shrink: 0;
  overflow-x: auto;
  &::-webkit-scrollbar {
    height: 0;
  }
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  font-size: 12px;
  border-radius: $radius-sm;
  cursor: pointer;
  color: $text-secondary;
  white-space: nowrap;
  border: 1px solid transparent;
  &:hover {
    background: $primary-bg;
    color: $primary;
  }
  &.active {
    background: $primary;
    color: #fff;
    .tag-close {
      color: rgba(255, 255, 255, 0.8);
      &:hover {
        color: #fff;
      }
    }
  }
}

.tag-close {
  font-size: 12px;
  margin-left: 2px;
  &:hover {
    color: $danger;
  }
}
</style>
