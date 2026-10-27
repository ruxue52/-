<script>
export default {
  onLaunch: function () {
    // 应用启动：可在此做全局初始化（如检查登录态）
    console.log('晓镜书阁 · 移动端启动')
  },
  onShow: function () {},
  onHide: function () {}
}
</script>

<style>
/* 全局基础样式 */
page {
  background-color: #f4f6fb;
  color: #1f2733;
  font-size: 28rpx;
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
}
.container {
  padding: 24rpx;
  box-sizing: border-box;
}
.card {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(31, 87, 196, 0.06);
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}
.muted { color: #6b7688; font-size: 24rpx; }
.mt8 { margin-top: 16rpx; }
.mt16 { margin-top: 32rpx; }
.row { display: flex; align-items: center; }
.spacer { flex: 1; }
.center { text-align: center; }
</style>