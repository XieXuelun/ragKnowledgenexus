import { defineStore } from "pinia";
import { ref } from "vue";
import type { RouteRecordNormalized } from "vue-router";

export interface TagView {
  name: string;
  path: string;
  title: string;
}

export const useTagsViewStore = defineStore("tagsView", () => {
  const visitedViews = ref<TagView[]>([
    { name: "Dashboard", path: "/dashboard", title: "工作台" },
  ]);

  function addView(route: RouteRecordNormalized) {
    const { name, path, meta } = route;
    if (!name || name === "Login") return;
    const exists = visitedViews.value.some((v) => v.name === String(name));
    if (!exists) {
      visitedViews.value.push({
        name: String(name),
        path,
        title: (meta?.title as string) || String(name),
      });
    }
  }

  function removeView(name: string) {
    visitedViews.value = visitedViews.value.filter((v) => v.name !== name);
  }

  return { visitedViews, addView, removeView };
});
