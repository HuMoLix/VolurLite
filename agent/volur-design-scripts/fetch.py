import os
import uuid
from PIL import Image
from pycocotools.coco import COCO
from tqdm import tqdm

# ==================== 配置项 ====================
ANNOTATION_FILE = "./annotations/instances_val2017.json"
IMAGE_DIR = "./val2017"
OUTPUT_DIR = "./captcha_dataset"

# 需要提取的目标分类（COCO 官方标准分类名）
TARGET_CATEGORIES = [
    "traffic light",  # 红绿灯
    "bicycle",        # 自行车
    "bus",            # 公交车
    "car",            # 汽车
    "motorcycle",     # 摩托车
    "fire hydrant",   # 消火栓
    "cat",            # 猫
    "dog"             # 狗
]

# 质量控制参数
MIN_WIDTH = 60          # 目标最小宽度（过滤过小物体）
MIN_HEIGHT = 60         # 目标最小高度
MIN_AREA = 3600         # 最小像素面积
PADDING_RATIO = 0.15    # 向外扩展边距比例（避免贴边太死，保留周围环境）
MAX_PER_CATEGORY = 500  # 每个类别最大提取数量
# ===============================================


def crop_coco_objects():
    coco = COCO(ANNOTATION_FILE)
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # 遍历每个指定的标签
    for cat_name in TARGET_CATEGORIES:
        cat_ids = coco.getCatIds(catNms=[cat_name])
        if not cat_ids:
            print(f"[-] 未找到分类: {cat_name}，跳过")
            continue
        
        cat_id = cat_ids[0]
        # 创建类别专属目录
        clean_dir_name = cat_name.replace(" ", "_")
        cat_output_dir = os.path.join(OUTPUT_DIR, clean_dir_name)
        os.makedirs(cat_output_dir, exist_ok=True)

        ann_ids = coco.getAnnIds(catIds=[cat_id], iscrowd=False)
        annotations = coco.loadAnns(ann_ids)

        saved_count = 0
        print(f"\n[+] 开始处理分类 [{cat_name}]，符合条件的标注总数: {len(annotations)}")

        for ann in tqdm(annotations, desc=f"Cropping {cat_name}"):
            if saved_count >= MAX_PER_CATEGORY:
                break

            # COCO bbox 格式: [x_min, y_min, width, height]
            x, y, w, h = ann["bbox"]

            # 1. 尺寸与面积阈值过滤
            if w < MIN_WIDTH or h < MIN_HEIGHT or (w * h) < MIN_AREA:
                continue

            # 2. 长宽比过滤（避免过于细长导致切图变形严重）
            aspect_ratio = w / h
            if aspect_ratio < 0.25 or aspect_ratio > 4.0:
                continue

            # 读取原始图像
            img_info = coco.loadImgs(ann["image_id"])[0]
            img_path = os.path.join(IMAGE_DIR, img_info["file_name"])

            if not os.path.exists(img_path):
                continue

            try:
                with Image.open(img_path) as img:
                    img_w, img_h = img.size

                    # 计算带 Padding 的裁剪框
                    pad_x = w * PADDING_RATIO
                    pad_y = h * PADDING_RATIO

                    left = max(0, int(x - pad_x))
                    top = max(0, int(y - pad_y))
                    right = min(img_w, int(x + w + pad_x))
                    bottom = min(img_h, int(y + h + pad_y))

                    # 3. 截取目标区域并转为 RGB 格式
                    cropped_img = img.crop((left, top, right, bottom)).convert("RGB")

                    # 统一缩放为验证码标准规格（例如 150x150）
                    resized_img = cropped_img.resize((150, 150), Image.Resampling.LANCZOS)

                    # 命名格式: 随机UUID.jpg（防哈希碰撞与原名泄露）
                    file_name = f"{uuid.uuid4().hex[:12]}.jpg"
                    save_path = os.path.join(cat_output_dir, file_name)
                    resized_img.save(save_path, "JPEG", quality=90)

                    saved_count += 1

            except Exception as e:
                # 忽略破损或不可读的图片
                continue

        print(f"[✓] 分类 [{cat_name}] 切图完成，共入库: {saved_count} 张。")


if __name__ == "__main__":
    crop_coco_objects()