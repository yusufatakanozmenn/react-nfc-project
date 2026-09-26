export const isAdmin = (user) => user?.role === "ADMIN";
export const homePath = (user) => isAdmin(user) ? "/" : "/cards";
