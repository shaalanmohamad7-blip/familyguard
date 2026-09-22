import { PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";

const prisma = new PrismaClient();

const DEMO_EMAIL = "demo@familyguard.app";
const DEMO_PASSWORD = "DemoParent123!";

async function main() {
  const existingUser = await prisma.user.findUnique({ where: { email: DEMO_EMAIL } });
  if (existingUser) {
    console.log(`Demo user already exists (${DEMO_EMAIL}). Skipping seed.`);
    return;
  }

  const passwordHash = await bcrypt.hash(DEMO_PASSWORD, 10);

  const user = await prisma.user.create({
    data: {
      email: DEMO_EMAIL,
      passwordHash,
      name: "Demo Parent",
    },
  });

  const family = await prisma.family.create({
    data: {
      name: "The Demo Family",
      isDemo: true,
      retentionDays: 90,
    },
  });

  await prisma.familyMember.create({
    data: { userId: user.id, familyId: family.id, role: "PARENT" },
  });

  // --- Devices ---
  const androidDevice = await prisma.device.create({
    data: {
      familyId: family.id,
      label: "Amira's phone (Android)",
      platform: "ANDROID",
      status: "ACTIVE",
      lastSeenAt: new Date(Date.now() - 4 * 60 * 1000),
      batteryPct: 76,
      appVersion: "1.4.0",
      permissions: {
        locationFg: true,
        locationBg: true,
        usageAccess: true,
        notifications: true,
      },
    },
  });

  const iosDevice = await prisma.device.create({
    data: {
      familyId: family.id,
      label: "Yousef's iPhone",
      platform: "IOS",
      status: "ACTIVE",
      lastSeenAt: new Date(Date.now() - 32 * 60 * 1000),
      batteryPct: 41,
      appVersion: "1.4.0",
      permissions: {
        locationFg: true,
        locationBg: true,
        usageAccess: false,
        notifications: true,
      },
    },
  });

  const offlineDevice = await prisma.device.create({
    data: {
      familyId: family.id,
      label: "Old tablet (offline)",
      platform: "ANDROID",
      status: "OFFLINE",
      lastSeenAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
      batteryPct: 12,
      appVersion: "1.2.0",
      permissions: {
        locationFg: true,
        locationBg: false,
        usageAccess: true,
        notifications: false,
      },
    },
  });

  // --- Geofences (Riyadh-area sample coordinates) ---
  const schoolGeofence = await prisma.geofence.create({
    data: {
      familyId: family.id,
      name: "School",
      lat: 24.7255,
      lng: 46.6349,
      radiusM: 150,
      alertOnArrival: true,
      alertOnDeparture: true,
    },
  });

  await prisma.geofence.create({
    data: {
      familyId: family.id,
      name: "Home",
      lat: 24.7136,
      lng: 46.6753,
      radiusM: 100,
      alertOnArrival: true,
      alertOnDeparture: false,
    },
  });

  // --- Location history (last 24h, walking Android device around Riyadh) ---
  const baseLat = 24.7136;
  const baseLng = 46.6753;
  const locationPoints = [];
  for (let i = 24; i >= 0; i--) {
    const capturedAt = new Date(Date.now() - i * 60 * 60 * 1000);
    locationPoints.push({
      deviceId: androidDevice.id,
      lat: baseLat + Math.sin(i / 3) * 0.01,
      lng: baseLng + Math.cos(i / 3) * 0.01,
      accuracyM: 8 + (i % 5),
      capturedAt,
      source: "BACKGROUND",
    });
  }
  await prisma.locationPoint.createMany({ data: locationPoints });

  await prisma.locationPoint.create({
    data: {
      deviceId: iosDevice.id,
      lat: 24.7255,
      lng: 46.6349,
      accuracyM: 12,
      capturedAt: new Date(Date.now() - 20 * 60 * 1000),
      source: "CHECK_IN",
    },
  });

  // --- Usage records: last 7 days, Android device only ---
  const apps = [
    { pkg: "com.instagram.android", label: "Instagram" },
    { pkg: "com.google.android.youtube", label: "YouTube" },
    { pkg: "com.whatsapp", label: "WhatsApp" },
    { pkg: "com.roblox.client", label: "Roblox" },
    { pkg: "com.duolingo", label: "Duolingo" },
  ];
  const usageRecords = [];
  for (let i = 6; i >= 0; i--) {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() - i);
    for (const app of apps) {
      const minutes = Math.max(0, Math.round(10 + Math.random() * 50 - i * 2));
      usageRecords.push({
        deviceId: androidDevice.id,
        date,
        appPackage: app.pkg,
        appLabel: app.label,
        minutes,
      });
    }
  }
  await prisma.usageRecord.createMany({ data: usageRecords });

  await prisma.limitRule.create({
    data: {
      deviceId: androidDevice.id,
      dailyLimitMinutes: 120,
      windowStart: "07:00",
      windowEnd: "20:00",
      daysOfWeek: [0, 1, 2, 3, 4, 5, 6],
      bedtimeStart: "21:00",
      bedtimeEnd: "06:30",
    },
  });

  // --- Alerts ---
  await prisma.alert.create({
    data: {
      familyId: family.id,
      deviceId: androidDevice.id,
      type: "GEOFENCE_ARRIVAL",
      payload: { geofenceId: schoolGeofence.id, geofenceName: "School" },
      createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000),
      acknowledgedAt: new Date(Date.now() - 5 * 60 * 60 * 1000 + 10 * 60 * 1000),
    },
  });

  await prisma.alert.create({
    data: {
      familyId: family.id,
      deviceId: offlineDevice.id,
      type: "OFFLINE",
      payload: { lastSeenAt: offlineDevice.lastSeenAt },
      createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
    },
  });

  await prisma.alert.create({
    data: {
      familyId: family.id,
      deviceId: iosDevice.id,
      type: "LOW_BATTERY",
      payload: { batteryPct: 15 },
      createdAt: new Date(Date.now() - 60 * 60 * 1000),
    },
  });

  // An unacknowledged SOS so the alerts feed demonstrates the urgent state.
  await prisma.alert.create({
    data: {
      familyId: family.id,
      deviceId: iosDevice.id,
      type: "SOS",
      payload: {
        lat: 24.7255,
        lng: 46.6349,
        accuracyM: 15,
        note: "Sample SOS alert for demo purposes.",
      },
      createdAt: new Date(Date.now() - 15 * 60 * 1000),
    },
  });

  console.log("Seed complete.");
  console.log(`Demo login: ${DEMO_EMAIL} / ${DEMO_PASSWORD}`);
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
