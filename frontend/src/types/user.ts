export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  email: string;
  phone: string;
  avatarUrl: string;
  role: number;
  deptId: number;
  deptName: string;
  status: number;
  createTime: string;
  updateTime?: string;
}

export interface UserListItem {
  id: number;
  username: string;
  nickname: string;
  email: string;
  avatarUrl: string;
  role: number;
  deptId: number;
  deptName: string;
  status: number;
  createTime: string;
  lastLoginTime?: string;
}

export interface Department {
  id: number;
  name: string;
  parentId: number;
  sort?: number;
  children?: Department[];
}
