import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { isAdminSession } from "@/lib/admin";

export const dynamic = "force-dynamic";

// Lists parent accounts awaiting the owner's approval. Owner-only.
export async function GET() {
  if (!(await isAdminSession())) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const users = await prisma.user.findMany({
    where: { status: "PENDING" },
    orderBy: { createdAt: "asc" },
    select: {
      id: true,
      name: true,
      email: true,
      createdAt: true,
      memberships: {
        select: { family: { select: { name: true } } },
        take: 1,
      },
    },
  });

  const list = users.map((u) => ({
    id: u.id,
    name: u.name,
    email: u.email,
    createdAt: u.createdAt,
    familyName: u.memberships[0]?.family.name ?? null,
  }));

  return NextResponse.json({ users: list });
}
