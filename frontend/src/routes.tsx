// src/routes.tsx
import { RouteObject } from "react-router-dom";
import StoragePage from "./pages/StoragePage";

const routes: RouteObject[] = [
    {
        path: "/",
        element: <StoragePage />,
    },
    // 추후 확장 가능:
    // { path: "/login", element: <LoginPage /> },
];

export default routes;