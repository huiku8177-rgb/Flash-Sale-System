import { createRouter, createWebHashHistory } from "vue-router";
import AppShellView from "../views/AppShellView.vue";
import AccountInfoView from "../views/AccountInfoView.vue";
import CartView from "../views/CartView.vue";
import CheckoutView from "../views/CheckoutView.vue";
import FlashSaleView from "../views/FlashSaleView.vue";
import HomeView from "../views/HomeView.vue";
import LoginView from "../views/LoginView.vue";
import OrdersCenterView from "../views/OrdersCenterView.vue";
import PasswordSecurityView from "../views/PasswordSecurityView.vue";
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
      path: "/register",
      name: "register",
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
      path: "/orders",
      redirect: "/app/profile/orders"
    },
    {
      path: "/account/profile",
      redirect: "/app/profile/account"
    },
    {
      path: "/account/security",
      redirect: "/app/profile/security"
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
        },
        {
          path: "profile/orders",
          name: "app-orders",
          component: OrdersCenterView,
          meta: {
            requiresAuth: true
          }
        },
        {
          path: "profile/account",
          name: "app-account-profile",
          component: AccountInfoView,
          meta: {
            requiresAuth: true
          }
        },
        {
          path: "profile/security",
          name: "app-account-security",
          component: PasswordSecurityView,
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
      name: "login",
      query: {
        redirect: to.fullPath
      }
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
