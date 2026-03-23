<script setup>
import { inject, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  createUserAddress,
  deleteUserAddress,
  setDefaultUserAddress,
  updateUserAddress
} from "../api/address";
import { formatCurrency } from "../utils/format";

const mallApp = inject("mallApp");
const router = useRouter();

const dialogVisible = ref(false);
const dialogLoading = ref(false);
const dialogMode = ref("create");
const editingAddressId = ref(null);
const addressForm = reactive({
  receiver: "",
  mobile: "",
  detail: "",
  isDefault: false
});

onMounted(() => {
  mallApp.loadAddresses();
});

function backToProfile() {
  router.push({ name: "app-profile" });
}

function resetAddressForm() {
  addressForm.receiver = "";
  addressForm.mobile = "";
  addressForm.detail = "";
  addressForm.isDefault = false;
  editingAddressId.value = null;
}

function openCreateDialog() {
  dialogMode.value = "create";
  resetAddressForm();
  dialogVisible.value = true;
}

function openEditDialog(address) {
  dialogMode.value = "edit";
  editingAddressId.value = address.id;
  addressForm.receiver = address.receiver ?? "";
  addressForm.mobile = address.mobile ?? "";
  addressForm.detail = address.detail ?? "";
  addressForm.isDefault = Boolean(address.isDefault);
  dialogVisible.value = true;
}

async function submitAddress() {
  const payload = {
    receiver: addressForm.receiver.trim(),
    mobile: addressForm.mobile.trim(),
    detail: addressForm.detail.trim(),
    isDefault: Boolean(addressForm.isDefault)
  };

  if (!payload.receiver || !payload.mobile || !payload.detail) {
    ElMessage.warning("请完整填写收货人、手机号和详细地址");
    return;
  }

  dialogLoading.value = true;
  try {
    if (dialogMode.value === "create") {
      await createUserAddress(payload);
      ElMessage.success("收货地址新增成功");
    } else {
      await updateUserAddress(editingAddressId.value, payload);
      ElMessage.success("收货地址修改成功");
    }

    dialogVisible.value = false;
    resetAddressForm();
    await mallApp.loadAddresses();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    dialogLoading.value = false;
  }
}

async function handleDelete(address) {
  try {
    await ElMessageBox.confirm(
      `确定删除地址“${address.receiver} / ${address.mobile}”吗？`,
      "删除地址",
      {
        type: "warning",
        confirmButtonText: "删除",
        cancelButtonText: "取消"
      }
    );

    await deleteUserAddress(address.id);
    ElMessage.success("收货地址已删除");
    await mallApp.loadAddresses();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error.message || "删除地址失败");
    }
  }
}

async function handleSetDefault(address) {
  if (address.isDefault) {
    return;
  }

  try {
    await setDefaultUserAddress(address.id);
    ElMessage.success("默认地址已更新");
    await mallApp.loadAddresses();
  } catch (error) {
    ElMessage.error(error.message);
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="profile-hub-hero section-card">
      <div class="profile-hub-copy">
        <p class="eyebrow">Account Profile</p>
        <h2>账户信息与地址管理</h2>
        <p>把账户资料和地址簿拆开整理后，个人中心会更像成熟电商站点，也更方便后续接入默认地址和结算页联动。</p>
      </div>
      <div class="profile-page-badge">
        <span>当前用户</span>
        <strong>{{ mallApp.profileDisplayName }}</strong>
      </div>
    </section>

    <section class="section-card">
      <div class="section-head">
        <div>
          <p class="eyebrow">Account Snapshot</p>
          <h3>账户概览</h3>
        </div>
        <el-button text @click="backToProfile">返回个人中心</el-button>
      </div>

      <div class="account-panel-grid">
        <article class="account-panel-card">
          <span>用户名</span>
          <strong>{{ mallApp.profile?.username || mallApp.authState.username }}</strong>
        </article>
        <article class="account-panel-card">
          <span>用户 ID</span>
          <strong>{{ mallApp.profile?.id || mallApp.authState.userId }}</strong>
        </article>
        <article class="account-panel-card">
          <span>普通订单</span>
          <strong>{{ mallApp.orderStats.normal }}</strong>
        </article>
        <article class="account-panel-card">
          <span>秒杀订单</span>
          <strong>{{ mallApp.orderStats.seckill }}</strong>
        </article>
        <article class="account-panel-card">
          <span>待支付订单</span>
          <strong>{{ mallApp.orderStats.created }}</strong>
        </article>
        <article class="account-panel-card">
          <span>购物车金额</span>
          <strong>{{ formatCurrency(mallApp.cartSummary.total) }}</strong>
        </article>
      </div>
    </section>

    <section class="section-card">
      <div class="section-head">
        <div>
          <p class="eyebrow">Address Book</p>
          <h3>收货地址</h3>
        </div>
        <el-button type="primary" @click="openCreateDialog">新增地址</el-button>
      </div>

      <div v-if="mallApp.addresses.length" class="address-book-grid">
        <article
          v-for="address in mallApp.addresses"
          :key="address.id"
          class="address-card"
          :class="{ 'address-card-default': address.isDefault }"
        >
          <div class="address-card-head">
            <div>
              <strong>{{ address.receiver }}</strong>
              <span>{{ address.mobile }}</span>
            </div>
            <el-tag v-if="address.isDefault" type="success" effect="light">默认地址</el-tag>
          </div>

          <p>{{ address.detail }}</p>

          <div class="address-card-actions">
            <el-button text @click="openEditDialog(address)">编辑</el-button>
            <el-button text @click="handleSetDefault(address)">设为默认</el-button>
            <el-button text type="danger" @click="handleDelete(address)">删除</el-button>
          </div>
        </article>
      </div>

      <el-empty
        v-else
        description="当前还没有已保存地址，建议先新增一条默认地址，结算时就可以直接下拉选择。"
      />
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增收货地址' : '修改收货地址'"
      width="560px"
      destroy-on-close
    >
      <el-form label-position="top" class="address-manage-form">
        <el-form-item label="收货人">
          <el-input v-model="addressForm.receiver" maxlength="64" placeholder="例如 张三" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="addressForm.mobile" maxlength="11" placeholder="例如 13800000000" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input
            v-model="addressForm.detail"
            type="textarea"
            :rows="4"
            maxlength="255"
            placeholder="例如 深圳市南山区科技园某路某号"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="addressForm.isDefault">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-actions-right">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="dialogLoading" @click="submitAddress">
            {{ dialogMode === "create" ? "保存地址" : "更新地址" }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
