import { createRouter, createWebHashHistory } from "vue-router";
import AppShellView from "../views/AppShellView.vue";
import CartView from "../views/CartView.vue";
import CheckoutView from "../views/CheckoutView.vue";
import FlashSaleView from "../views/FlashSaleView.vue";
import HomeView from "../views/HomeView.vue";
import LoginView from "../views/LoginView.vue";
import ProfileView from "../views/ProfileView.vue";
import { authState } from "../stores/auth";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      redirect: "/app/home"
    },
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: {
        guestOnly: true
      }
    },
    {
      path: "/checkout",
      name: "checkout",
      component: CheckoutView,
      meta: {
        requiresAuth: true
      }
    },
    {
      path: "/app",
      component: AppShellView,
      children: [
        {
          path: "",
          redirect: "/app/home"
        },
        {
          path: "home",
          name: "app-home",
          component: HomeView
        },
        {
          path: "flash",
          name: "app-flash",
          component: FlashSaleView
        },
        {
          path: "cart",
          name: "app-cart",
          component: CartView,
          meta: {
            requiresAuth: true
          }
        },
        {
          path: "profile",
          name: "app-profile",
          component: ProfileView,
          meta: {
            requiresAuth: true
          }
        }
      ]
    }
  ]
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authState.token) {
    return {
      name: "login"
    };
  }

  if (to.meta.guestOnly && authState.token) {
    return {
      name: "app-home"
    };
  }

  return true;
});

export default router;
