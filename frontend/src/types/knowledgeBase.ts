export interface KnowledgeBase {
  id: string;
  name: string;
  description: string;
  icon?: string;
  visibility: number;
  creatorId: number;
  creatorName?: string;
  docCount: number;
  qaCount?: number;
  viewCount?: number;
  favoriteCount?: number;
  isFavorite?: boolean;
  createTime: string;
  updateTime: string;
}

export interface DocumentItem {
  id: string;
  kbId: string;
  title: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  parseStatus: number;
  parseError?: string;
  chunkCount: number;
  knowledgeName: string;
  createTime: string;
}

export interface Conversation {
  id: string;
  title: string;
  kbId: string;
  messageCount?: number;
  createTime: string;
  updateTime?: string;
}
