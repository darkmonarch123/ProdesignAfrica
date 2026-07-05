import { Suspense } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls, Environment, GizmoHelper, GizmoViewport } from '@react-three/drei';
import BuildingScene from './BuildingScene';
import SunLight from './SunLight';
import { useEditorStore } from '../store/editorStore';

export default function ThreeDView() {
  const canvasState = useEditorStore((s) => s.canvasState);

  return (
    <div className="w-full h-full bg-[#0d1b2a]">
      <Canvas shadows camera={{ position: [12, 10, 12], fov: 45 }}>
        <Suspense fallback={null}>
          <SunLight />
          <BuildingScene canvasState={canvasState} />
          <Environment preset="city" />
          <OrbitControls makeDefault enableDamping dampingFactor={0.08} maxPolarAngle={Math.PI / 2.05} />
          <GizmoHelper alignment="bottom-right" margin={[64, 64]}>
            <GizmoViewport axisColors={['#E24B4A', '#047857', '#185FA5']} labelColor="white" />
          </GizmoHelper>
        </Suspense>
      </Canvas>
    </div>
  );
}
